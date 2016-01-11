package util;

/**
 * Created by clwang on 1/4/16.
 */
public class IndentionManagement {

    static String basicIndent = "  ";

    public static String addIndention(String input, int indentLv) {
        String indent = "";
        for (int i = 0; i < indentLv; i ++) {
            indent += basicIndent;
        }

        return indent + input.replace("\r\n", "\r\n" + indent);
    }

    public static String basicIndent() {
        return basicIndent;
    };
}
