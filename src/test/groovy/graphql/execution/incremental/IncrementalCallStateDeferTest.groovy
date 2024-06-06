package graphql.execution.incremental

import graphql.ExecutionResultImpl
import graphql.execution.ResultPath
import graphql.execution.pubsub.CapturingSubscriber
import graphql.incremental.DelayedIncrementalPartialResult
import org.awaitility.Awaitility
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class IncrementalCallStateDeferTest extends Specification {

    def "emits N deferred calls - ordering depends on call latency"() {
        given:
        def incrementalCallState = new IncrementalCallState()
        incrementalCallState.enqueue(offThread("A", 100, "/field/path")) // <-- will finish last
        incrementalCallState.enqueue(offThread("B", 50, "/field/path")) // <-- will finish second
        incrementalCallState.enqueue(offThread("C", 10, "/field/path")) // <-- will finish first

        when:
        List<DelayedIncrementalPartialResult> results = startAndWaitCalls(incrementalCallState)

        then:
        assertResultsSizeAndHasNextRule(3, results)
        results[0].incremental[0].data["c"] == "C"
        results[1].incremental[0].data["b"] == "B"
        results[2].incremental[0].data["a"] == "A"
    }

    def "calls within calls are enqueued correctly"() {
        given:
        def incrementalCallState = new IncrementalCallState()
        incrementalCallState.enqueue(offThreadCallWithinCall(incrementalCallState, "A", "A_Child", 500, "/a"))
        incrementalCallState.enqueue(offThreadCallWithinCall(incrementalCallState, "B", "B_Child", 300, "/b"))
        incrementalCallState.enqueue(offThreadCallWithinCall(incrementalCallState, "C", "C_Child", 100, "/c"))

        when:
        List<DelayedIncrementalPartialResult> results = startAndWaitCalls(incrementalCallState)

        then:
        assertResultsSizeAndHasNextRule(6, results)
        results[0].incremental[0].data["c"] == "C"
        results[1].incremental[0].data["c_child"] == "C_Child"
        results[2].incremental[0].data["b"] == "B"
        results[3].incremental[0].data["a"] == "A"
        results[4].incremental[0].data["b_child"] == "B_Child"
        results[5].incremental[0].data["a_child"] == "A_Child"
    }

    def "stops at first exception encountered"() {
        given:
        def incrementalCallState = new IncrementalCallState()
        incrementalCallState.enqueue(offThread("A", 100, "/field/path"))
        incrementalCallState.enqueue(offThread("Bang", 50, "/field/path")) // <-- will throw exception
        incrementalCallState.enqueue(offThread("C", 10, "/field/path"))

        when:
        def subscriber = new CapturingSubscriber<DelayedIncrementalPartialResult>() {
            @Override
            void onComplete() {
                assert false, "This should not be called!"
            }
        }
        incrementalCallState.startDeferredCalls().subscribe(subscriber)

        Awaitility.await().untilTrue(subscriber.isDone())

        def results = subscriber.getEvents()
        def thrown = subscriber.getThrowable()

        then:
        thrown.message == "java.lang.RuntimeException: Bang"
        results[0].incremental[0].data["c"] == "C"
    }

    def "you can cancel the subscription"() {
        given:
        def incrementalCallState = new IncrementalCallState()
        incrementalCallState.enqueue(offThread("A", 100, "/field/path")) // <-- will finish last
        incrementalCallState.enqueue(offThread("B", 50, "/field/path")) // <-- will finish second
        incrementalCallState.enqueue(offThread("C", 10, "/field/path")) // <-- will finish first

        when:
        def subscriber = new CapturingSubscriber<DelayedIncrementalPartialResult>() {
            @Override
            void onNext(DelayedIncrementalPartialResult executionResult) {
                this.getEvents().add(executionResult)
                subscription.cancel()
                this.isDone().set(true)
            }
        }
        incrementalCallState.startDeferredCalls().subscribe(subscriber)

        Awaitility.await().untilTrue(subscriber.isDone())
        def results = subscriber.getEvents()

        then:
        results.size() == 1
        results[0].incremental[0].data["c"] == "C"
        // Cancelling the subscription will result in an invalid state.
        // The last result item will have "hasNext=true" (but there will be no next)
        results[0].hasNext
    }

    def "you can't subscribe twice"() {
        given:
        def incrementalCallState = new IncrementalCallState()
        incrementalCallState.enqueue(offThread("A", 100, "/field/path"))
        incrementalCallState.enqueue(offThread("Bang", 50, "/field/path")) // <-- will finish second
        incrementalCallState.enqueue(offThread("C", 10, "/field/path")) // <-- will finish first

        when:
        def subscriber1 = new CapturingSubscriber<DelayedIncrementalPartialResult>()
        def subscriber2 = new CapturingSubscriber<DelayedIncrementalPartialResult>()
        incrementalCallState.startDeferredCalls().subscribe(subscriber1)
        incrementalCallState.startDeferredCalls().subscribe(subscriber2)

        then:
        subscriber2.throwable != null
        subscriber2.throwable.message == "This publisher only supports one subscriber"
    }

    def "indicates if there are any defers present"() {
        given:
        def incrementalCallState = new IncrementalCallState()

        when:
        def deferPresent1 = incrementalCallState.getIncrementalCallsDetected()

        then:
        !deferPresent1

        when:
        incrementalCallState.enqueue(offThread("A", 100, "/field/path"))
        def deferPresent2 = incrementalCallState.getIncrementalCallsDetected()

        then:
        deferPresent2
    }

    def "multiple fields are part of the same call"() {
        given: "a DeferredCall that contains resolution of multiple fields"
        def call1 = CompletableFuture.supplyAsync({
            Thread.sleep(10)
            new DeferredFragmentCall.FieldWithExecutionResult("call1", new ExecutionResultImpl("Call 1", []))
        })

        def call2 = CompletableFuture.supplyAsync({
            Thread.sleep(100)
            new DeferredFragmentCall.FieldWithExecutionResult("call2", new ExecutionResultImpl("Call 2", []))
        })

        def deferredCall = new DeferredFragmentCall(null, ResultPath.parse("/field/path"), [call1, call2], new DeferredCallContext())

        when:
        def incrementalCallState = new IncrementalCallState()
        incrementalCallState.enqueue(deferredCall)

        def results = startAndWaitCalls(incrementalCallState)

        then:
        assertResultsSizeAndHasNextRule(1, results)
        results[0].incremental[0].data["call1"] == "Call 1"
        results[0].incremental[0].data["call2"] == "Call 2"
    }

    def "race conditions should not impact the calculation of the hasNext value"() {
        given: "calls that have the same sleepTime"
        def incrementalCallState = new IncrementalCallState()
        incrementalCallState.enqueue(offThread("A", 10, "/field/path")) // <-- will finish last
        incrementalCallState.enqueue(offThread("B", 10, "/field/path")) // <-- will finish second
        incrementalCallState.enqueue(offThread("C", 10, "/field/path")) // <-- will finish first

        when:
        List<DelayedIncrementalPartialResult> results = startAndWaitCalls(incrementalCallState)

        then: "hasNext placement should be deterministic - only the last event published should have 'hasNext=true'"
        assertResultsSizeAndHasNextRule(3, results)

        then: "but the actual order or publish events is non-deterministic - they all have the same latency (sleepTime)."
        results.any { it.incremental[0].data["a"] == "A" }
        results.any { it.incremental[0].data["b"] == "B" }
        results.any { it.incremental[0].data["c"] == "C" }
    }

    private static DeferredFragmentCall offThread(String data, int sleepTime, String path) {
        def callSupplier = CompletableFuture.supplyAsync({
                    Thread.sleep(sleepTime)
                    if (data == "Bang") {
                        throw new RuntimeException(data)
                    }
                    new DeferredFragmentCall.FieldWithExecutionResult(data.toLowerCase(), new ExecutionResultImpl(data, []))
                })

        return new DeferredFragmentCall(null, ResultPath.parse(path), [callSupplier], new DeferredCallContext())
    }

    private static DeferredFragmentCall offThreadCallWithinCall(IncrementalCallState incrementalCallState, String dataParent, String dataChild, int sleepTime, String path) {
        def callSupplier = CompletableFuture.supplyAsync({
                    Thread.sleep(sleepTime)
                    incrementalCallState.enqueue(offThread(dataChild, sleepTime, path))
                    new DeferredFragmentCall.FieldWithExecutionResult(dataParent.toLowerCase(), new ExecutionResultImpl(dataParent, []))
                })

        return new DeferredFragmentCall(null, ResultPath.parse("/field/path"), [callSupplier], new DeferredCallContext())
    }

    private static void assertResultsSizeAndHasNextRule(int expectedSize, List<DelayedIncrementalPartialResult> results) {
        assert results.size() == expectedSize

        for (def i = 0; i < results.size(); i++) {
            def isLastResult = i == results.size() - 1
            def hasNext = results[i].hasNext()

            assert (hasNext && !isLastResult)
                    || (!hasNext && isLastResult)
        }
    }

    private static List<DelayedIncrementalPartialResult> startAndWaitCalls(IncrementalCallState incrementalCallState) {
        def subscriber = new CapturingSubscriber<DelayedIncrementalPartialResult>()

        incrementalCallState.startDeferredCalls().subscribe(subscriber)

        Awaitility.await().untilTrue(subscriber.isDone())
        return subscriber.getEvents()
    }
}
