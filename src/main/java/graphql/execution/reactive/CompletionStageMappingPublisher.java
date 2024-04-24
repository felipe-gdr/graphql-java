package graphql.execution.reactive;

import graphql.Internal;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * A reactive Publisher that bridges over another Publisher of `D` and maps the results
 * to type `U` via a CompletionStage, handling errors in that stage
 *
 * @param <D> the downstream type
 * @param <U> the upstream type to be mapped to
 */
@Internal
public class CompletionStageMappingPublisher<D, U> implements Publisher<D> {
    private final Publisher<U> upstreamPublisher;
    private final Function<U, CompletionStage<D>> mapper;

    /**
     * You need the following :
     *
     * @param upstreamPublisher an upstream source of data
     * @param mapper            a mapper function that turns upstream data into a promise of mapped D downstream data
     */
    public CompletionStageMappingPublisher(Publisher<U> upstreamPublisher, Function<U, CompletionStage<D>> mapper) {
        this.upstreamPublisher = upstreamPublisher;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super D> downstreamSubscriber) {
        upstreamPublisher.subscribe(new CompletionStageSubscriber<>(mapper, downstreamSubscriber));
    }

    /**
     * Get instance of an upstreamPublisher
     *
     * @return upstream instance of {@link Publisher}
     */
    public Publisher<U> getUpstreamPublisher() {
        return upstreamPublisher;
    }

}
