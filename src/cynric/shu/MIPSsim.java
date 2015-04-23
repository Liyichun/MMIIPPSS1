package cynric.shu;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MIPSsim {

    public static void main(String[] args) {
        Disassembler disassembler = new Disassembler();
        Simulator simulator = new Simulator();
        final String _inputFilePath = args[0];
        final String _outputFilePath = "./disassembler.txt";
        final String _resultFilePath = "./simulation.txt";
        File file = new File(_inputFilePath);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                Instruction instruction = new Instruction(line);
                disassembler.disassemble(instruction);
                simulator.addInstr(instruction);
                if ("BREAK".equals(instruction.getName())) {
                    System.out.println("break");
                    break;
                }
            }
            simulator.printInstr(_outputFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Simulator {
    List<Instruction> instrList = new ArrayList<>();

    public void addInstr(Instruction instr) {
        this.instrList.add(instr);
    }

    public void printInstr(String outputFilePath) {
        try {
            FileWriter writer = new FileWriter(new File(outputFilePath));
            for (Instruction i : instrList) {
                writer.write(i.getPrintValue() + "\n");
            }
            writer.flush();
            writer.close();
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
    private static final String category3Pattern = "111(?<rs>\\d{5})(?<rt>\\d{5})(?<opcode>\\d{3})(?<imValue>\\d{16})";
    private static Map<String, Category1InstrHandler> codeCategory1InstrMap = new HashMap<>();
    private static boolean isBreak;

    public Disassembler() {
        init();
    }

    private static void init() {
        initCategory1CodeInstrMap();
    }

    private static void initCategory1CodeInstrMap() {
        codeCategory1InstrMap.put("000", new J("J", "000000(?<instrIndex>\\d{26})"));
        codeCategory1InstrMap.put("010", new BEQ("BEQ", "000010(?<rs>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"));
        codeCategory1InstrMap.put("100", new BGTZ("BGTZ", "000100(?<rs>\\d{5})\\d{5}(?<offset>\\d{16})"));
        codeCategory1InstrMap.put("101", new BREAK("BREAK", "\\d{32}"));
        codeCategory1InstrMap.put("110", new SW("SW", "000110(?<base>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"));
        codeCategory1InstrMap.put("111", new LW("LW", "000111(?<base>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"));
    }

    private static void processCategory1(Instruction instruction) {
        Pattern pattern = Pattern.compile(category1Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            String opcode = matcher.group("opcode");
            Category1InstrHandler handler = codeCategory1InstrMap.get(opcode);
            handler.handle(instruction);
        }
    }

    private static void processCategory2(Instruction instruction) {
        Pattern pattern = Pattern.compile(category2Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            Category2InstrHandler handler = new Category2InstrHandler(matcher);
            handler.handle(instruction);
        }
    }

    private static void processCategory3(Instruction instruction) {
        Pattern pattern = Pattern.compile(category3Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            Category3InstrHandler handler = new Category3InstrHandler(matcher);
            handler.handle(instruction);
        }
    }

    public void disassemble(Instruction instruction) {
        String head = instruction.getContent().substring(0, 3);
        switch (head) {
            case "000":
                processCategory1(instruction);
                break;
            case "110":
                processCategory2(instruction);
                break;
            case "111":
                processCategory3(instruction);
                break;
            default:
                break;
        }
    }
}


class Instruction {
    private int[] args;
    private String content;
    private String name;
    private String printValue;

    public Instruction(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getPrintValue() {
        return printValue;
    }

    public void setPrintValue(String printValue) {
        this.printValue = printValue;
    }

    public int[] getArgs() {
        return args;
    }

    public void setArgs(int[] args) {
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

abstract class InstrHandler {
    Instruction instruction;
    Pattern pattern;
    Matcher matcher;
    String name;

    public InstrHandler() {
    }

    public Integer parseInt(String s) {
        return Integer.parseInt(s);
    }

    public void handle(Instruction instruction) {
        this.instruction = instruction;
        instruction.setName(this.name);
        handleCode(matcher);
    }

    public Integer binToDec(String binary) {
        return Integer.valueOf(binary, 2);
    }

    public abstract void handleCode(Matcher matcher);
}

abstract class Category1InstrHandler extends InstrHandler {
    String regex;

    public Category1InstrHandler(String name, String regex) {
        this.name = name;
        this.regex = regex;
        pattern = Pattern.compile(regex);
    }

    @Override
    public void handle(Instruction instruction) {
        this.instruction = instruction;
        instruction.setName(this.name);
        handleCode(pattern.matcher(instruction.getContent()));
    }
}

class Category2InstrHandler extends InstrHandler {
    private Map<String, String> map = new HashMap<>();

    public Category2InstrHandler(Matcher matcher) {
        super();
        this.matcher = matcher;
        map.put("000", "ADD");
        map.put("001", "SUB");
        map.put("010", "MUL");
        map.put("011", "AND");
        map.put("100", "OR");
        map.put("101", "XOR");
        map.put("110", "NOR");
    }

    @Override
    public void handleCode(Matcher matcher) {
        String opcode = this.matcher.group("opcode");
        String rs = this.matcher.group("rs");
        String rt = this.matcher.group("rt");
        String rd = this.matcher.group("rd");
        this.name = map.get(opcode);
        String printValue = name + " R" + binToDec(rd) + ", R" + binToDec(rs) + ", R" + binToDec(rt);
        instruction.setPrintValue(printValue);

    }
}

class Category3InstrHandler extends InstrHandler {
    private Map<String, String> map = new HashMap<>();

    public Category3InstrHandler(Matcher matcher) {
        super();
        this.matcher = matcher;
        map.put("000", "ADDI");
        map.put("001", "ANDI");
        map.put("010", "ORI");
        map.put("011", "XORI");
    }

    @Override
    public void handleCode(Matcher matcher) {
        String opcode = this.matcher.group("opcode");
        String rs = this.matcher.group("rs");
        String rt = this.matcher.group("rt");
        String imValue = this.matcher.group("imValue");
        this.name = map.get(opcode);
        String printValue = name + " R" + binToDec(rt) + ", R" + binToDec(rs) + ", #" + binToDec(imValue);
        instruction.setPrintValue(printValue);
    }
}


class J extends Category1InstrHandler {
    public J(String name, String regex) {
        super(name, regex);
    }

    @Override
    public void handleCode(Matcher matcher) {
        String instrIndex = matcher.group("instrIndex");
        Integer target = (binToDec(instrIndex)) << 2;
        instruction.setPrintValue(this.name + " #" + target);
    }
}

class BEQ extends Category1InstrHandler {
    public BEQ(String name, String regex) {
        super(name, regex);
    }

    @Override
    public void handleCode(Matcher matcher) {
        String rs = matcher.group("rs");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        Integer target = (binToDec(offset)) << 2;
        String printValue = this.name + " R" + binToDec(rs) + ", R" + binToDec(rt) + ", #" + target;
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{parseInt(rs), parseInt(rt), parseInt(offset)});
    }
}

class BGTZ extends Category1InstrHandler {

    public BGTZ(String name, String regex) {
        super(name, regex);
    }

    @Override
    public void handleCode(Matcher matcher) {
        String rs = matcher.group("rs");
        String offset = matcher.group("offset");
        Integer target = (binToDec(offset)) << 2;
        String printValue = this.name + " R" + binToDec(rs) + ", #" + target;
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{parseInt(rs), parseInt(offset)});
    }
}

class BREAK extends Category1InstrHandler {
    public BREAK(String name, String regex) {
        super(name, regex);
    }

    @Override
    public void handleCode(Matcher matcher) {

        instruction.setPrintValue("BREAK");

    }
}

class SW extends Category1InstrHandler {
    public SW(String name, String regex) {
        super(name, regex);
    }

    @Override
    public void handleCode(Matcher matcher) {
        String base = matcher.group("base");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        String printValue = this.name + " R" + binToDec(rt) + ", " + binToDec(offset) + "(R" + binToDec(base) + ")";
        instruction.setPrintValue(printValue);
    }
}

class LW extends Category1InstrHandler {
    public LW(String name, String regex) {
        super(name, regex);
    }

    @Override
    public void handleCode(Matcher matcher) {
        String base = matcher.group("base");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        String printValue = this.name + " R" + binToDec(rt) + ", " + binToDec(offset) + "(R" + binToDec(base) + ")";
        instruction.setPrintValue(printValue);
    }
}



