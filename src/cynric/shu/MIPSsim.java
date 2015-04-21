package cynric.shu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum InstructionType {category1, category2, category3}

public class MIPSsim {

    private static final String category1Pattern = "000(?<opcode>\\d{3})";

    public static void main(String[] args) {
//        final String _inputFilePath = args[0];

        String a = "000111";
        Pattern pattern = Pattern.compile(category1Pattern);
        Matcher matcher = pattern.matcher(a);
        if (matcher.matches()) {
            System.out.println(matcher.group("opcode"));
        }
    }


    public void disassemble() {

    }

    private void processCategory1(String content) {

    }
}

class Instruction {
    String head;

    String content;

    InstructionType insType = null;

    public Instruction(String content) {
        this.content = content;
        this.head = content.substring(0, 4);
        switch (head) {
            case "000":
                insType = InstructionType.category1;
                break;
            case "110":
                insType = InstructionType.category2;
                break;
            case "111":
                insType = InstructionType.category3;
                break;
            default:
                break;
        }
    }

}
