package CrawlerAndQueryEngine;
import java.util.*;

/**
 * A query engine which holds an underlying web index and can answer textual queries with a
 * collection of relevant pages.
 */
public class WebQueryEngine {
    //The index that the engine is based on
    private WebIndex index;

    public WebQueryEngine(WebIndex index){
        this.index = index;
    }

    /**
     * Returns a WebQueryEngine that uses the given Index to construct answers to queries.
     *
     * @param index The WebIndex this WebQueryEngine should use.
     * @return A WebQueryEngine ready to be queried.
     */
    public static WebQueryEngine fromIndex(WebIndex index) {
        // TODO: Implement this!
        return new WebQueryEngine(index);
    }

    /**
     * Returns a Collection of URLs (as Strings) of web pages satisfying the query expression.
     *
     * @param query A query expression.
     * @return A collection of web pages satisfying the query.
     */
    //Parses a given query and returns the associated Collection of Pages
    public Set<Page> query(String query) {
        // TODO: Implement this!
        //Tokenize query
        List<String> queries = tokenize(query);
        if(queries.size() == 0){
            return new HashSet<>();
        }
        if(queries.size() == 1){
            return index.search(queries.get(0));
        }
        //Use Shunting-yard to parse tokenized query
        return evaluatePostfix(postFix(queries));
    }

    //Tokenize query into operators, operands, and parentheses
    public List<String> tokenize(String query) {
        List<String> tokens = new LinkedList<>();
        StringBuilder token = new StringBuilder();
        for(int i = 0; i < query.length(); i++) {
            switch (query.charAt(i)) {
                //Parentheses Token (Open)
                case '(':
                    if(token.length() > 0) {
                        tokens.add(token.toString());
                        token = new StringBuilder();
                    }
                    tokens.add("(");
                    break;
                //Parentheses Token (Close)
                case ')':
                    if(token.length() > 0) {
                        tokens.add(token.toString());
                        token = new StringBuilder();
                    }
                    tokens.add(")");
                    break;
                //Operand Token (Phrase)
                case '\"':
                    if(token.length() > 0) {
                        tokens.add(token.toString());
                        token = new StringBuilder();
                    }
                    i++;
                    token.append('\"');
                    while(i < query.length() && query.charAt(i) != '\"'){
                        token.append(Character.toLowerCase(query.charAt(i)));
                        i++;
                    }
                    token.append('\"');
                    tokens.add(token.toString());
                    token = new StringBuilder();
                    break;
                //Operator Token (and)
                case '&':
                    if(token.length() > 0) {
                        tokens.add(token.toString());
                        token = new StringBuilder();
                    }
                    tokens.add("&");
                    break;
                //Operator Token (or)
                case '|':
                    if(token.length() > 0) {
                        tokens.add(token.toString());
                        token = new StringBuilder();
                    }
                    tokens.add("|");
                    break;
                //Operand Token (not)
                case '!':
                    if(token.length() == 0 && i + 1 < query.length() &&
                            Character.isLetterOrDigit(query.charAt(i + 1))) {
                        token.append('!');
                    }
                    break;
                //Operand Token (word)
                default:
                    if(Character.isLetterOrDigit(query.charAt(i))) {
                        token.append(Character.toLowerCase(query.charAt(i)));
                    } else {
                        if(token.length() > 0) {
                            tokens.add(token.toString());
                            token = new StringBuilder();
                        }
                    }
                    break;
            }
        }

        if(token.length() > 0) {
            tokens.add(token.toString());
        }

        return tokens;
    }

    private boolean isHigherPrec(String op, String sub) {
        return ((sub.equals("&")) || (sub.equals("|"))) && !(op.equals("&") && sub.equals("|"));
    }

    //Shunting-yard algorithm
    public List<String> postFix(List<String> tokens) {
        List<String> output = new LinkedList<>();
        Deque<String> stack = new LinkedList<>();

        //Used to deal with implicit and operations
        boolean lastOperand = false;

        for(String token: tokens) {
            if(token.equals("&") || token.equals("|")){
                lastOperand = false;
                while(!stack.isEmpty() && isHigherPrec(token, stack.peek())) {
                    output.add(stack.pop());
                }
                stack.push(token);
            } else if (token.equals("(")){
                if(lastOperand){
                    //Add implicit and
                    lastOperand = false;
                    while(!stack.isEmpty() && isHigherPrec("&", stack.peek())) {
                        output.add(stack.pop());
                    }
                    stack.push("&");
                }

                stack.push(token);
            } else if (token.equals(")")) {
                lastOperand = true;
                while(!stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                stack.pop();
            } else {
                if(lastOperand){
                    //Add implicit and
                    while(!stack.isEmpty() && isHigherPrec("&", stack.peek())) {
                        output.add(stack.pop());
                    }
                    stack.push("&");
                }

                lastOperand = true;
                output.add(token);
            }
        }

        while(!stack.isEmpty()){
            output.add(stack.pop());
        }
        return output;
    }


    //Evaluate postfix tokens after Shunting-yard
    private Set<Page> evaluatePostfix(List<String> tokens) {
        Stack<String> operandStack = new Stack<>();
        Set<Page> output = null;
        for(String element: tokens) {
            if(element.equals("&")){
                if(output == null){
                    output = and(operandStack.pop(), operandStack.pop());
                } else {
                    and(operandStack.pop(), output);
                }
            } else if (element.equals("|")){
                if(output == null) {
                    output = or(operandStack.pop(), operandStack.pop());
                } else {
                    or(operandStack.pop(), output);
                }
            } else {
                operandStack.push(element);
            }
        }
        if(output == null){
            return new HashSet<>();
        }
        return output;
    }

    private Set<Page> and(String lhs, String rhs) {
        if(lhs.charAt(0) == '!') {
            Set<Page> output = index.search(rhs);
            output.removeAll(index.search(lhs.substring(1)));
            return output;
        }

        if(rhs.charAt(0) == '!') {
            Set<Page> output = index.search(lhs);
            output.removeAll(index.search(rhs.substring(1)));
            System.out.println("here");
            return output;
        }

        if(lhs.charAt(0) != '\"') {
            Set<Page> output = index.searchWord(lhs);
            if(rhs.charAt(0) != '\"') {
                output.retainAll(index.searchWord(rhs));
            } else {
                output = index.searchPhraseAdd(rhs, output);
            }
            return output;
        }
        if(rhs.charAt(0) != '\"') {
            Set<Page> output = index.searchWord(rhs);
            if(lhs.charAt(0) != '\"') {
                output.retainAll(index.searchWord(lhs));
            } else {
                output = index.searchPhraseAdd(lhs, output);
            }
            return output;
        }

        Set<Page> output = index.search(lhs);
        output = index.searchPhraseAdd(rhs, output);

        return output;
    }

    private void and(String token, Set<Page> pageSet) {
        if(token.charAt(0) == '!') {
            pageSet.removeAll(index.searchWord(token.substring(1)));
        } else if (token.charAt(0) == '"') {
            index.searchPhraseAdd(token, pageSet);
        } else {
            pageSet.retainAll(index.searchWord(token));
        }
    }

    private Set<Page> or(String lhs, String rhs) {
        if(lhs.charAt(0) == '!' && rhs.charAt(0) == '!') {
            Set<Page> output = index.search(lhs.substring(1));
            output.retainAll(index.search(rhs.substring(1)));
            return index.inverse(output);
        }

        if(lhs.charAt(0) == '!') {
            Set<Page> output = index.search(lhs.substring(1));
            output.removeAll(index.search(rhs));
            return index.inverse(output);
        }

        if(rhs.charAt(0) == '!') {
            Set<Page> output = index.search(rhs.substring(1));
            output.removeAll(index.search(lhs));
            return index.inverse(output);
        }

        if(lhs.charAt(0) != '\"') {
            Set<Page> output = index.searchWord(lhs);
            if(rhs.charAt(0) != '\"') {
                output.addAll(index.searchWord(rhs));
            } else {
                output.addAll(index.searchPhraseRemove(rhs, output));
            }
            return output;
        }
        if(rhs.charAt(0) != '\"') {
            Set<Page> output = index.searchWord(rhs);
            if(lhs.charAt(0) != '\"') {
                output.addAll(index.searchWord(lhs));
            } else {
                output.addAll(index.searchPhraseRemove(lhs, output));
            }
            return output;
        }

        Set<Page> output = index.search(lhs);
        output.addAll(index.searchPhraseRemove(lhs, output));
        return output;
    }

    private void or(String token, Set<Page> pageSet) {
        if (token.charAt(0) == '"') {
            pageSet.addAll(index.searchPhraseRemove(token, pageSet));
        } else {
            pageSet.addAll(index.searchWord(token));
        }
    }
}
