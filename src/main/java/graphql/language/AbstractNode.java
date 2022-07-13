package graphql.language;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.Assert;
import graphql.PublicApi;
import graphql.collect.ImmutableKit;

import java.util.List;
import java.util.Map;

import static graphql.collect.ImmutableKit.map;

@PublicApi
public abstract class AbstractNode<T extends Node> implements Node<T> {

    private final SourceLocation sourceLocation;
    private final ImmutableList<Comment> comments;
    private final IgnoredChars ignoredChars;
    private final ImmutableMap<String, String> additionalData;

    /**
     * @deprecated The concept of comments in AST Nodes has been deprecated. use {@link AbstractNode#AbstractNode(SourceLocation, IgnoredChars)}
     */
    @Deprecated
    public AbstractNode(SourceLocation sourceLocation, List<Comment> comments, IgnoredChars ignoredChars) {
        this(sourceLocation, comments, ignoredChars, ImmutableKit.emptyMap());
    }

    public AbstractNode(SourceLocation sourceLocation, IgnoredChars ignoredChars) {
        this(sourceLocation, ImmutableKit.emptyList(), ignoredChars, ImmutableKit.emptyMap());
    }

    /**
     * @deprecated The concept of comments in AST Nodes has been deprecated. use {@link AbstractNode#AbstractNode(SourceLocation, IgnoredChars, Map)}
     */
    @Deprecated
    public AbstractNode(SourceLocation sourceLocation, List<Comment> comments, IgnoredChars ignoredChars, Map<String, String> additionalData) {
        Assert.assertNotNull(comments, () -> "comments can't be null");
        Assert.assertNotNull(ignoredChars, () -> "ignoredChars can't be null");
        Assert.assertNotNull(additionalData, () -> "additionalData can't be null");

        this.sourceLocation = sourceLocation;
        this.additionalData = ImmutableMap.copyOf(additionalData);
        this.comments = ImmutableList.copyOf(comments);
        this.ignoredChars = ignoredChars;
    }

    public AbstractNode(SourceLocation sourceLocation, IgnoredChars ignoredChars, Map<String, String> additionalData) {
        this(sourceLocation, ImmutableKit.emptyList(), ignoredChars, additionalData);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Get comments associated with the node.
     *
     * The concept of comments as a piece of text associated with a schema element has been deprecated. Use descriptions
     * instead. They're the official mechanism for documenting schema elements, and show up in introspection.
     *
     * Comments are still valid and can be valuable as a means to internally document decisions, etc. However it doesn't
     * make sense to associated them with schema elements.
     *
     * @deprecated use {@link AbstractDescribedNode#description} for documenting schema elements, and
     * use {@link graphql.parser.Parser#parseComments(String)} (and its variations) for extracting comments from a
     * document.
     *
     * @return a list of {@link Comment} associated with the node.
     */
    @Deprecated
    @Override
    public List<Comment> getComments() {
        return comments;
    }

    @Override
    public IgnoredChars getIgnoredChars() {
        return ignoredChars;
    }


    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    @SuppressWarnings("unchecked")
    protected <V extends Node> V deepCopy(V nullableObj) {
        if (nullableObj == null) {
            return null;
        }
        return (V) nullableObj.deepCopy();
    }

    @SuppressWarnings("unchecked")
    protected <V extends Node> List<V> deepCopy(List<? extends Node> list) {
        if (list == null) {
            return null;
        }
        return map(list, n -> (V) n.deepCopy());
    }
}
