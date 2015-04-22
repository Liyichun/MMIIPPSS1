package cynric.shu;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum InstructionType {category1, category2, category3}

public class MIPSsim {


    public static void main(String[] args) {
        Disassembler disassembler = new Disassembler();
        final String _inputFilePath = args[0];
        File file = new File(_inputFilePath);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                Instruction instruction = new Instruction(line);
                disassembler.disassemble(instruction);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}

class Disassembler {
    private static final String category1Pattern = "000(?<opcode>\\d{3})\\d{26}";
    private static final String category2Pattern = "110(?<rs>\\d{5})(?<rt>\\d{5})(?<opcode>\\d{3})(?<rd>\\d{5})\\d{11}";
    private static final String category3Pattern = "111(?<rs>\\d{5})(?<rt>\\d{5})(?<opcode>\\d{3})(?<im_value>\\d{16})";
    private static Map<String, Category1InstrHandler> codeCategory1InstrMap = new HashMap<>();
    private static boolean isBreak;

    public Disassembler() {
        init();
    }

    private static void init() {
        initCategory1CodeInstrMap();
    }

    private static void initCategory1CodeInstrMap() {
        codeCategory1InstrMap.put("000", new Instr_J_handler());
    }

    private static String processCategory1(Instruction instruction) {
        String content = instruction.getContent();
        Pattern pattern = Pattern.compile(category1Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            String opcode = matcher.group("opcode");
            Category1InstrHandler instr = codeCategory1InstrMap.get(opcode);
            if (instr != null) {
                return instr.handle(content);
            } else {
                return "";
            }
        }

        return "";
    }

    public void disassemble(Instruction instruction) {
        switch (instruction.insType) {
            case category1:
                System.out.println(processCategory1(instruction));
                break;
            case category2:
                break;
            case category3:
                break;
            default:
                break;
        }
    }
}


class Instruction {
    String head;
    String content;
    String name;
    String opcode;
    InstructionType insType = null;

    public Instruction() {
    }

    public Instruction(String content) {
        this.content = content;
        this.head = content.substring(0, 3);
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

    public String getContent() {
        return content;
    }
}

abstract class Category1InstrHandler {
    Pattern pattern;
    Matcher matcher;
    String regex;
    String name;

    public Category1InstrHandler() {
        super();
    }

    public Category1InstrHandler(String name, String regex) {
        this.name = name;
        this.regex = regex;
        pattern = Pattern.compile(regex);
    }

    public String handle(String content) {
        matcher = pattern.matcher(content);
        if (matcher.matches()) {
            return handleCode(matcher);
        } else {
            return "regex match error";
        }
    }

    public abstract String handleCode(Matcher matcher);

}

class Instr_J_handler extends Category1InstrHandler {
    public Instr_J_handler() {
        super("J", "000000(?<instrIndex>\\d{26})");
    }

    @Override
    public String handleCode(Matcher matcher) {
        return this.name + " target";
    }
}

class Instr_BEQ_handler extends Category1InstrHandler {
    public Instr_BEQ_handler() {
        super("BEQ", "000010(?<rs>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})");
    }

    @Override
    public String handleCode(Matcher matcher) {
        String rs = matcher.group("rs");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        return null;
    }
}

