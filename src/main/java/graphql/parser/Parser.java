package graphql.parser;

import graphql.PublicApi;
import graphql.language.Comment;
import graphql.language.Document;
import graphql.language.SourceLocation;
import graphql.language.Type;
import graphql.language.Value;
import graphql.parser.antlr.GraphqlBaseListener;
import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.BiFunction;

/**
 * This can parse graphql syntax, both Query syntax and Schema Definition Language (SDL) syntax, into an
 * Abstract Syntax Tree (AST) represented by a {@link Document}
 * <p>
 * You should not generally need to call this class as the {@link graphql.GraphQL} code sets this up for you
 * but if you are doing specific graphql utilities this class is essential.
 *
 * Graphql syntax has a series of characters, such as spaces, new lines and commas that are not considered relevant
 * to the syntax.  However they can be captured and associated with the AST elements they belong to.
 *
 * This costs more memory but for certain use cases (like editors) this maybe be useful.  We have chosen to no capture
 * ignored characters by default but you can turn this on, either per parse or statically for the whole JVM
 * via {@link ParserOptions#setDefaultParserOptions(ParserOptions)} ()}}
 *
 * @see graphql.language.IgnoredChar
 */
@PublicApi
public class Parser {

    /**
     * Parses a string input into a graphql AST {@link Document}
     *
     * @param input the input to parse
     *
     * @return an AST {@link Document}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public static Document parse(String input) throws InvalidSyntaxException {
        return new Parser().parseDocument(input);
    }

    /**
     * Parses a string input into a graphql AST {@link Value}
     *
     * @param input the input to parse
     *
     * @return an AST {@link Value}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public static Value<?> parseValue(String input) throws InvalidSyntaxException {
        return new Parser().parseValueImpl(input);
    }

    /**
     * Parses a string input into a graphql AST Type
     *
     * @param input the input to parse
     *
     * @return an AST {@link Type}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public static Type<?> parseType(String input) throws InvalidSyntaxException {
        return new Parser().parseTypeImpl(input);
    }

    /**
     * Parses a string input into a graphql AST {@link Document}
     *
     * @param input the input to parse
     *
     * @return an AST {@link Document}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public Document parseDocument(String input) throws InvalidSyntaxException {
        return parseDocument(input, (ParserOptions) null);
    }

    /**
     * Parses a string input into a graphql AST {@link Document}
     *
     * @param input      the input to parse
     * @param sourceName - the name to attribute to the input text in {@link SourceLocation#getSourceName()}
     *
     * @return an AST {@link Document}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public Document parseDocument(String input, String sourceName) throws InvalidSyntaxException {
        MultiSourceReader multiSourceReader = stringToReader(input, sourceName);
        return parseDocument(multiSourceReader);
    }

    /**
     * Parses a string input into a graphql AST {@link Document}
     *
     * @param input         the input to parse
     * @param parserOptions the parser options
     *
     * @return an AST {@link Document}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public Document parseDocument(String input, ParserOptions parserOptions) throws InvalidSyntaxException {
        MultiSourceReader multiSourceReader = stringToReader(input, null);
        return parseDocument(multiSourceReader, parserOptions);
    }

    // TODO: Javadoc
    public List<Comment> parseComments(String input) {
        return parseComments(input, null);
    }

    // TODO: Javadoc
    public List<Comment> parseComments(String input, ParserOptions parserOptions) throws InvalidSyntaxException {
        MultiSourceReader multiSourceReader = stringToReader(input, null);
        return parseComments(multiSourceReader, parserOptions);
    }

    // TODO: Javadoc
    public List<Comment> parseComments(Reader reader, ParserOptions parserOptions) {
        return parseCommentsImpl(reader, parserOptions);
    }

    // TODO: Javadoc
    public List<Comment> parseComments(Reader reader) {
        return parseCommentsImpl(reader, null);
    }

    /**
     * Parses reader  input into a graphql AST {@link Document}
     *
     * @param reader the reader input to parse
     *
     * @return an AST {@link Document}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public Document parseDocument(Reader reader) throws InvalidSyntaxException {
        return parseDocumentImpl(reader, null);
    }

    /**
     * Parses reader  input into a graphql AST {@link Document}
     *
     * @param reader        the reader input to parse
     * @param parserOptions the parser options
     *
     * @return an AST {@link Document}
     *
     * @throws InvalidSyntaxException if the input is not valid graphql syntax
     */
    public Document parseDocument(Reader reader, ParserOptions parserOptions) throws InvalidSyntaxException {
        return parseDocumentImpl(reader, parserOptions);
    }

    private List<Comment> parseCommentsImpl(Reader reader, ParserOptions parserOptions) {
        BiFunction<GraphqlParser, GraphqlAntlrToLanguage, NodeFunctionReturn<List<Comment>>> nodeFunction = (parser, toLanguage) -> {
            GraphqlParser.DocumentContext documentContext = parser.document();

            List<Comment> allComments = toLanguage.getAllComments();

            return new NodeFunctionReturn<>(documentContext, allComments);
        };
        return parseImpl(reader, nodeFunction, parserOptions);
    }

    private Document parseDocumentImpl(Reader reader, ParserOptions parserOptions) throws InvalidSyntaxException {
        BiFunction<GraphqlParser, GraphqlAntlrToLanguage, NodeFunctionReturn<Document>> nodeFunction = (parser, toLanguage) -> {
            GraphqlParser.DocumentContext documentContext = parser.document();
            Document doc = toLanguage.createDocument(documentContext);

            return new NodeFunctionReturn<>(documentContext, doc);
        };
        return parseImpl(reader, nodeFunction, parserOptions);
    }

    private Value<?> parseValueImpl(String input) throws InvalidSyntaxException {
        BiFunction<GraphqlParser, GraphqlAntlrToLanguage, NodeFunctionReturn<Value<?>>> nodeFunction = (parser, toLanguage) -> {
            GraphqlParser.ValueContext documentContext = parser.value();
            Value<?> value = toLanguage.createValue(documentContext);
            return new NodeFunctionReturn<>(documentContext, value);
        };
        MultiSourceReader multiSourceReader = stringToReader(input, null);
        return parseImpl(multiSourceReader, nodeFunction, null);
    }

    private Type<?> parseTypeImpl(String input) throws InvalidSyntaxException {
        BiFunction<GraphqlParser, GraphqlAntlrToLanguage, NodeFunctionReturn<Type<?>>> nodeFunction = (parser, toLanguage) -> {
            final GraphqlParser.TypeContext documentContext = parser.type();
            Type<?> value = toLanguage.createType(documentContext);
            return new NodeFunctionReturn<>(documentContext, value);
        };
        MultiSourceReader multiSourceReader = stringToReader(input, null);
        return parseImpl(multiSourceReader, nodeFunction, null);
    }

    private <T> T parseImpl(Reader reader, BiFunction<GraphqlParser, GraphqlAntlrToLanguage, NodeFunctionReturn<T>> nodeFunction, ParserOptions parserOptions) throws InvalidSyntaxException {
        MultiSourceReader multiSourceReader;
        if (reader instanceof MultiSourceReader) {
            multiSourceReader = (MultiSourceReader) reader;
        } else {
            multiSourceReader = MultiSourceReader.newMultiSourceReader()
                    .reader(reader, null).build();
        }
        CodePointCharStream charStream;
        try {
            charStream = CharStreams.fromReader(multiSourceReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        GraphqlLexer lexer = new GraphqlLexer(charStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                SourceLocation sourceLocation = AntlrHelper.createSourceLocation(multiSourceReader, line, charPositionInLine);
                String preview = AntlrHelper.createPreview(multiSourceReader, line);
                throw new InvalidSyntaxException(sourceLocation, msg, preview, null, null);
            }
        });

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        GraphqlParser parser = new GraphqlParser(tokens);
        parser.removeErrorListeners();
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

        ExtendedBailStrategy bailStrategy = new ExtendedBailStrategy(multiSourceReader);
        parser.setErrorHandler(bailStrategy);

        // preserve old protected call semantics - remove at some point
        GraphqlAntlrToLanguage toLanguage = getAntlrToLanguage(tokens, multiSourceReader);
        if (toLanguage == null) {
            toLanguage = getAntlrToLanguage(tokens, multiSourceReader, parserOptions);
        }

        setupParserListener(multiSourceReader, parser, toLanguage);


        //
        // parsing starts ...... now!
        //
        NodeFunctionReturn<T> nodeFunctionReturn = nodeFunction.apply(parser, toLanguage);
        ParserRuleContext parserRuleContext = nodeFunctionReturn.context;
        T nodeFunctionData = nodeFunctionReturn.data;

        Token stop = parserRuleContext.getStop();
        List<Token> allTokens = tokens.getTokens();
        if (stop != null && allTokens != null && !allTokens.isEmpty()) {
            Token last = allTokens.get(allTokens.size() - 1);
            //
            // do we have more tokens in the stream than we consumed in the parse?
            // if yes then it's invalid.  We make sure it's the same channel
            boolean notEOF = last.getType() != Token.EOF;
            boolean lastGreaterThanDocument = last.getTokenIndex() > stop.getTokenIndex();
            boolean sameChannel = last.getChannel() == stop.getChannel();
            if (notEOF && lastGreaterThanDocument && sameChannel) {
                throw bailStrategy.mkMoreTokensException(last);
            }
        }
        return nodeFunctionData;
    }

    private void setupParserListener(MultiSourceReader multiSourceReader, GraphqlParser parser, GraphqlAntlrToLanguage toLanguage) {
        ParserOptions parserOptions = toLanguage.getParserOptions();
        ParsingListener parsingListener = parserOptions.getParsingListener();
        int maxTokens = parserOptions.getMaxTokens();
        // prevent a billion laugh attacks by restricting how many tokens we allow
        ParseTreeListener listener = new GraphqlBaseListener() {
            int count = 0;

            @Override
            public void visitTerminal(TerminalNode node) {

                final Token token = node.getSymbol();
                parsingListener.onToken(new ParsingListener.Token() {
                    @Override
                    public String getText() {
                        return token == null ? null : token.getText();
                    }

                    @Override
                    public int getLine() {
                        return token == null ? -1 : token.getLine();
                    }

                    @Override
                    public int getCharPositionInLine() {
                        return token == null ? -1 : token.getCharPositionInLine();
                    }
                });

                count++;
                if (count > maxTokens) {
                    String msg = String.format("More than %d parse tokens have been presented. To prevent Denial Of Service attacks, parsing has been cancelled.", maxTokens);
                    SourceLocation sourceLocation = null;
                    String offendingToken = null;
                    if (token != null) {
                        offendingToken = node.getText();
                        sourceLocation = AntlrHelper.createSourceLocation(multiSourceReader, token.getLine(), token.getCharPositionInLine());
                    }

                    throw new ParseCancelledException(msg, sourceLocation, offendingToken);
                }
            }
        };
        parser.addParseListener(listener);
    }

    /**
     * Allows you to override the ANTLR to AST code.
     *
     * @param tokens            the token stream
     * @param multiSourceReader the source of the query document
     *
     * @return a new GraphqlAntlrToLanguage instance
     *
     * @deprecated - really should use {@link #getAntlrToLanguage(CommonTokenStream, MultiSourceReader, ParserOptions)}
     */
    @Deprecated
    protected GraphqlAntlrToLanguage getAntlrToLanguage(CommonTokenStream tokens, MultiSourceReader multiSourceReader) {
        return null;
    }

    /**
     * Allows you to override the ANTLR to AST code.
     *
     * @param tokens            the token stream
     * @param multiSourceReader the source of the query document
     * @param parserOptions     - the parser options
     *
     * @return a new GraphqlAntlrToLanguage instance
     */
    protected GraphqlAntlrToLanguage getAntlrToLanguage(CommonTokenStream tokens, MultiSourceReader multiSourceReader, ParserOptions parserOptions) {
        return new GraphqlAntlrToLanguage(tokens, multiSourceReader, parserOptions);
    }

    private MultiSourceReader stringToReader(String input, String sourceName) {
        return MultiSourceReader.newMultiSourceReader()
                .string(input, sourceName)
                .trackData(true)
                .build();
    }

    private static class NodeFunctionReturn<T> {
        private final ParserRuleContext context;
        private final T data;

        private NodeFunctionReturn(ParserRuleContext context, T data) {
            this.context = context;
            this.data = data;
        }
    }
}
