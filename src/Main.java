/**
 * Created by Lucius on 16/6/1.
 */

/**
 * <text>    -->   <stmtList> .
 * <stmtList>-->   <stmt>(； <stmt>)*
 * <stmt>    -->   <declare> | <output> | <calc>
 * <declare> -->   TYPE <varList>
 * <varList> -->   ID(，ID)*
 * <output>  -->   write (<calc>)
 * <calc>    -->   ID = <calc>  |  <expr>
 * <expr>    -->   <term> <expr_>
 * <expr_>   -->   (+|-) <term> <expr_> | ε
 * <term>    -->   <factor> <term_>
 * <term_>   -->   (*|/) <factor> <term_> | ε
 * <factor>  -->   —<factor> | ID | INT | FLOAT | (<calc>)
 **/

import java.io.*;
import java.util.*;
import java.lang.System;

class Tag { public final static int INT = 257, FLOAT= 258, TYPE = 259, ID = 260, ACT = 261; }

class Token {
    public final int tag, cur, row;
    public Token(int t, int c, int r) { tag = t; cur = c; row = r; }
    public String toString() { return String.valueOf((char)tag); }
}

class IntTok extends Token {
    public int value;
    public IntTok(int v, int c, int r) { super(Tag.INT, c, r); value = v; }
    public String toString() { return "number " + value; }
}

class FloatTok extends Token {
    public double value;
    public FloatTok(double v, int c, int r) { super(Tag.FLOAT, c, r); value = v; }
    public String toString() { return "number " + value; }
}

class Id extends Token {
    public String lexeme;
    public Id(String s, int c, int r) { super(Tag.ID, c, r); lexeme = s; }
    public String toString() { return "variable \"" + lexeme + "\""; }
}

class TypeTok extends Token {
    public String lexeme;
    public TypeTok(String s,int c, int r) { super(Tag.TYPE, c, r); lexeme = s; }
    public String toString() { return "Type-name"; }
}

class Act extends Token {
    public Act(int c, int r) { super(Tag.ACT, c, r); }
    public String toString() { return "Write function"; }
}

/* Lexer: Read a token and return it immediately */
class Lexer {
    Scanner in = new Scanner(System.in);
    private char peek;
    private int cur;
    public int row;
    private String buff;
    public void clear() { buff = ""; peek=' '; cur = 0; row = 1; }

    private char readChar() {
        if (cur >= buff.length()) {
            try {
                buff = in.nextLine();
                cur = 0;
                ++row;
            } catch (NoSuchElementException e) {
                System.out.println("读取完毕!");
                System.exit(0);
            }
        }
        return peek = buff.charAt(cur++);
    }

    public Token scan() {
        while (in != null && Character.isSpaceChar(peek)) {
            readChar();
        }

        if (Character.isDigit(peek)) {
            IntTok intTok = new IntTok(0, cur, row);
            for (; Character.isDigit(peek); readChar())
                intTok.value = 10 * intTok.value + Character.digit(peek, 10);
            if (peek == '.') {
                FloatTok floatTok = new FloatTok((double) intTok.value, cur, row);
                int decPlace = 1;
                while (Character.isDigit(readChar())) {
                    floatTok.value += (double)Character.digit(peek, 10) / (decPlace *= 10);
                }
                return floatTok;
            }
            return intTok;
        }
        if (Character.isLetter(peek) || peek == '_' ) {
            Id idTok = new Id(String.valueOf(peek), cur, row);

            while (Character.isLetterOrDigit(readChar()) || peek == '_') {
                idTok.lexeme += peek;
            }

            if (idTok.lexeme.equals("int")) {
                TypeTok typeTok = new TypeTok("int", cur, row);
                return typeTok;
            } else if (idTok.lexeme.equals("float")) {
                TypeTok typeTok = new TypeTok("float", cur, row);
                return typeTok;
            } else if (idTok.lexeme.equals("write")) {
                Act act = new Act(cur, row);
                return act;
            }
            return idTok;
        }

        Token tok = new Token(peek, cur, row);
        peek = ' ';
        return tok;
    }
}

/* Parser, the core of this calculator */
class Parser {
    public class ParsingException extends Exception {
        Token token;
        public ParsingException(String msg, Token t) {
            super(msg);
            token = t;
        }
    }

    private Lexer lex;
    private ArrayDeque<Token> heads = new ArrayDeque<>();
    private void  readTok() {
        if (heads.isEmpty()) {
            heads.addLast(lex.scan());
        }
    }
    private Token headTok() {
        if (heads.isEmpty()) { readTok(); }
            return heads.peek();
    }

    private void put_back(Token token) { heads.addFirst(token); }
    private HashMap<String, Token> variables = new HashMap<>();
    public StringWriter writer = new StringWriter();

    Token add(Token a, Token b) {
        Token tmp;
        if (a.tag == b.tag){
            if (a.tag == Tag.INT) {
                tmp = new IntTok(((IntTok)a).value + ((IntTok)b).value, a.cur, a.row);
            } else {
                tmp = new FloatTok(((FloatTok)a).value + ((FloatTok)b).value, a.cur, a.row);
            }
        } else if (a.tag == Tag.INT) {
            FloatTok ta = new FloatTok(((IntTok)a).value, a.cur, a.row);
            tmp = new FloatTok(ta.value + ((FloatTok)b).value, a.cur, a.row);
        } else {
            FloatTok tb = new FloatTok(((IntTok)b).value, a.cur, a.row);
            tmp = new FloatTok(((FloatTok)a).value + tb.value, a.cur, a.row);
        }
        return tmp;
    }
    Token sub(Token a, Token b) {
        Token tmp;
        if (a.tag == b.tag){
            if (a.tag == Tag.INT) {
                tmp = new IntTok(((IntTok)a).value - ((IntTok)b).value, a.cur, a.row);
            } else {
                tmp = new FloatTok(((FloatTok)a).value - ((FloatTok)b).value, a.cur, a.row);
            }
        } else if (a.tag == Tag.INT) {
            FloatTok ta = new FloatTok(((IntTok)a).value, a.cur, a.row);
            tmp = new FloatTok(ta.value - ((FloatTok)b).value, a.cur, a.row);
        } else {
            FloatTok tb = new FloatTok(((IntTok)b).value, a.cur, a.row);
            tmp = new FloatTok(((FloatTok)a).value - tb.value, a.cur, a.row);
        }
        return tmp;
    }
    Token mul(Token a, Token b) {
        Token tmp;
        if (a.tag == b.tag){
            if (a.tag == Tag.INT) {
                tmp = new IntTok(((IntTok)a).value * ((IntTok)b).value, a.cur, a.row);
            } else {
                tmp = new FloatTok(((FloatTok)a).value * ((FloatTok)b).value, a.cur, a.row);
            }
        } else if (a.tag == Tag.INT) {
            FloatTok ta = new FloatTok(((IntTok)a).value, a.cur, a.row);
            tmp = new FloatTok(ta.value * ((FloatTok)b).value, a.cur, a.row);
        } else {
            FloatTok tb = new FloatTok(((IntTok)b).value, a.cur, a.row);
            tmp = new FloatTok(((FloatTok)a).value * tb.value, a.cur, a.row);
        }
        return tmp;
    }
    Token div(Token a, Token b) throws ParsingException {
        Token tmp;
        if (a.tag == b.tag){
            if (a.tag == Tag.INT) {
                if (((IntTok)b).value != 0) {
                    tmp = new IntTok(((IntTok) a).value / ((IntTok) b).value, a.cur, a.row);
                } else {
                    throw new ParsingException("不能除0!", b);
                }
            } else {
                if (((FloatTok)b).value != 0.0) {
                    tmp = new FloatTok(((FloatTok) a).value / ((FloatTok) b).value, a.cur, a.row);
                } else {
                    throw new ParsingException("不能除0!", b);
                }
            }
        } else if (a.tag == Tag.INT) {
            if (((FloatTok)b).value != 0.0) {
                FloatTok ta = new FloatTok(((IntTok)a).value, a.cur, a.row);
                tmp = new FloatTok(ta.value / ((FloatTok)b).value, a.cur, a.row);
            } else {
                throw new ParsingException("不能除0!", b);
            }
        } else {
            if (((IntTok)b).value != 0) {
                FloatTok tb = new FloatTok(((IntTok)b).value, a.cur, a.row);
                tmp = new FloatTok(((FloatTok)a).value / tb.value, a.cur, a.row);
            } else {
                throw new ParsingException("不能除0!", b);
            }
        }
        return tmp;
    }
    boolean match(int tag) {
        if (headTok().tag == tag) {
            heads.removeFirst();
            readTok();
            return true;
        } else {
            return false;
        }
    }

    void text() throws ParsingException {
        switch (headTok().tag) {
            case Tag.INT:
            case Tag.FLOAT:
            case Tag.TYPE:
            case Tag.ID:
            case Tag.ACT:
            case '-':
            case '(':
                stmtList();
                if (headTok().tag != '.') {
                    throw new ParsingException("输入应该以.结尾!", headTok());
                }
                break;
            default:
                throw new ParsingException("开头不正确!", headTok());
        }
    }

    void stmtList() throws ParsingException {
        do {
            switch (headTok().tag) {
                case Tag.INT:
                case Tag.FLOAT:
                case Tag.TYPE:
                case Tag.ID:
                case Tag.ACT:
                case '-':
                case '(':
                    stmt();
                    break;
                default:
                    throw new ParsingException("语句开头不正确!", headTok());
            }
        } while (match(';'));
    }

    void stmt() throws ParsingException {
        switch (headTok().tag) {
            case Tag.INT:
            case Tag.FLOAT:
            case Tag.ID:
            case '-':
            case '(':
                calc();
                break;
            case Tag.TYPE:
                declare();
                break;
            case Tag.ACT:
                output();
                break;
            default:
                throw new ParsingException("语句开头不正确!", headTok());
        }
    }


    void declare() throws ParsingException {
        if (headTok().tag == Tag.TYPE) {
            final String type_name = ((TypeTok)headTok()).lexeme;
            heads.removeFirst();
            varlist(type_name);
        } else {
            throw new ParsingException("变量声明应该以变量名开头!", headTok());
        }
    }

    void varlist(final String typeName) throws ParsingException {
        do {
            if (headTok().tag != Tag.ID) {
                throw new ParsingException("变量列表应该由标识符组成!", headTok());
            }
            final String val_name = ((Id)headTok()).lexeme;
            if (variables.containsKey(val_name)) {
                throw new ParsingException("变量应该只声明一次!", headTok());
            }

            if (typeName.equals("int")) {
                variables.put(val_name, new IntTok(0, headTok().cur, headTok().row));
            } else if (typeName.equals("float")) {
                variables.put(val_name, new FloatTok(0.0, headTok().cur, headTok().row));
            } else {
                throw new ParsingException("不支持的类型!", headTok());
            }
            heads.removeFirst();
        } while (match(','));
    }

    void output() throws ParsingException {
        if (!match(Tag.ACT)) {
            throw new ParsingException("输出语句应该以write 开头", headTok());
        }
        if (!match('(')) {
            throw new ParsingException("应该有'('!", headTok());
        }
        Token val = calc();
        if (val.tag == Tag.INT) {
            writer.write(String.valueOf(((IntTok)val).value));
            writer.write("\n");
        } else {
            writer.write(String.valueOf(((FloatTok)val).value));
            writer.write("\n");
        }
        if (!match(')')) {
            throw new ParsingException("应该有')'!", headTok());
        }
    }

    Token calc() throws ParsingException {
        Token first = headTok();
        switch (first.tag) {
            case Tag.ID:
                heads.removeFirst();
                if (match('=')) {
                    Token val = calc();
                    if (!variables.containsKey(((Id) first).lexeme)) {
                        throw new ParsingException("未定义的标识符", first);
                    }
                    final Token t = variables.get(((Id)first).lexeme);
                    if (t.tag == Tag.INT) {
                        if (val.tag == Tag.FLOAT) {
                            throw new ParsingException("float 类型不能转换成 int 类型!", headTok());
                        } else {
                            ((IntTok)t).value = ((IntTok)val).value;
                        }
                    } else {
                        if (val.tag == Tag.INT) {
                            ((FloatTok)t).value = ((IntTok)val).value;
                        } else {
                            ((FloatTok)t).value = ((FloatTok)val).value;
                        }
                    }
                    return t;
                } else {
                    put_back(first);
                }
            case '-':
            case Tag.INT:
            case Tag.FLOAT:
            case '(':
                return expr();
            default:
                throw new ParsingException("算式中出现异常字符!", headTok());
        }
    }

    Token expr() throws ParsingException {
        Token term;
        switch (headTok().tag) {
            case Tag.INT:
            case Tag.FLOAT:
            case Tag.ID:
            case '-':
            case '(':
                term = term();
                return expr_(term);
            default:
                throw new ParsingException("表达式不应该由此字符开始!", headTok());
        }
    }

    Token expr_(final Token token) throws ParsingException {
        Token ahead = headTok();
        Token term;
        switch (ahead.tag) {
            case ')':
            case ';':
            case '.':
                return token;
            case '+':
            case '-':
                heads.removeFirst();
                term = term();
                if (ahead.tag == '+') {
                    return expr_(add(token, term));
                } else {
                    return expr_(sub(token, term));
                }
            default:
                throw new ParsingException("表达式结尾字符有误!", headTok());
        }
    }

    Token term() throws ParsingException {
        Token factor;
        switch (headTok().tag) {
            case Tag.INT:
            case Tag.FLOAT:
            case Tag.ID:
            case '-':
            case '(':
                factor = factor();
                return term_(factor);
            default:
                throw new ParsingException("加/减法缺运算数!", headTok());
        }
    }

    Token term_(final Token inherit) throws ParsingException {
        Token ahead = headTok();
        Token factor;
        switch (ahead.tag) {
            case '+':
            case '-':
            case ')':
            case ';':
            case '.':
                return inherit;
            case '*':
            case '/':
                heads.removeFirst();
                factor = factor();
                if (ahead.tag == '*') {
                    return term_(mul(inherit, factor));
                } else {
                    return term_(div(inherit, factor));
                }
            default:
                throw new ParsingException("项后应该是;或者.或者运算符!", ahead);
        }
    }

    Token factor() throws ParsingException {
        Token ahead = headTok();
        heads.removeFirst();
        switch (ahead.tag) {
            case Tag.INT:
            case Tag.FLOAT:
                return ahead;
            case Tag.ID:
                if (!variables.containsKey(((Id)ahead).lexeme)) {
                    throw new ParsingException("未定义的标识符!" ,ahead);
                }
                final Token t = variables.get(((Id)ahead).lexeme);
                if (t.tag == Tag.INT) {
                    return new IntTok(((IntTok)t).value, t.cur, t.row);
                } else {
                    return new FloatTok(((FloatTok)t).value, t.cur, t.row);
                }
            case '-':
                return sub(new IntTok(0, ahead.cur, ahead.row), factor());
            case '(':
                Token calc = calc();
                if (match(')')) {
                    return calc;
                } else {
                    throw new ParsingException("右括号缺失!", headTok());
                }
            default:
                throw new ParsingException("乘/除法中出现不明符号!", ahead);
        }
    }

    public Parser(Lexer l) {
        lex = l;
    }

    public String parse() throws ParsingException {
        heads.clear();
        lex.clear();
        readTok();
        text();
        return writer.toString();
    }
}

public class Main {
    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        Parser parser = new Parser(lexer);
        String ans;
            try {
                System.out.println("In>>");
                ans = parser.parse();
                System.out.println("Out>>");
                System.out.println(ans);
            } catch (Parser.ParsingException e) {
                System.out.println("错误! -- " + e.getMessage());
                System.out.println("发生在第" + e.token.row + "行" + "第" + e.token.cur + "列!");
            }
    }
}