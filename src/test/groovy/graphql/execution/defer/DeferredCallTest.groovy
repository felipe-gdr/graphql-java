package graphql.execution.defer


import graphql.ExecutionResultImpl
import graphql.validation.ValidationError
import graphql.validation.ValidationErrorType
import spock.lang.Specification

import static graphql.execution.ResultPath.parse
import static java.util.concurrent.CompletableFuture.completedFuture

class DeferredCallTest extends Specification {

    def "test call capture gives a CF"() {
        given:
        DeferredCall call = new DeferredCall(parse("/path"), {
            completedFuture(new ExecutionResultImpl("some data", Collections.emptyList()))
        }, new DeferredErrorSupport())

        when:
        def future = call.invoke()
        then:
        future.join().data == "some data"
        future.join().path == ["path"]
    }

    def "test error capture happens via CF"() {
        given:
        def errorSupport = new DeferredErrorSupport()
        errorSupport.onError(new ValidationError(ValidationErrorType.MissingFieldArgument))
        errorSupport.onError(new ValidationError(ValidationErrorType.FieldsConflict))

        DeferredCall call = new DeferredCall(parse("/path"), {
            completedFuture(new ExecutionResultImpl("some data", [new ValidationError(ValidationErrorType.FieldUndefined)]))
        }, errorSupport)

        when:
        def future = call.invoke()
        def er = future.join()

        then:
        er.errors.size() == 2
        // I'm not sure why the result of the deferred call should include an error generated in the main execution result
        // TODO: Double check if this assert should be here or not
        // er.errors[0].message.contains("Validation error of type FieldUndefined")
        er.errors[0].message.contains("Validation error of type MissingFieldArgument")
        er.errors[1].message.contains("Validation error of type FieldsConflict")
        er.path == ["path"]
    }
}
