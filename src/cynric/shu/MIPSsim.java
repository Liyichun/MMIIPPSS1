package cynric.shu;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MIPSsim {
    static final String _disassembleFilePath = "./disassembler.txt";
    static final String _simulationFilePath = "./simulation.txt";

    public static void main(String[] args) {
        Disassembler disassembler = new Disassembler();
        Simulator simulator = new Simulator();
        final String _inputFilePath = args[0];

        File file = new File(_inputFilePath);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                Instruction instruction = new Instruction(line);
                disassembler.disassemble(instruction);
                simulator.addInstr(instruction);
                if ("BREAK".equals(instruction.getName())) {
                    break;
                }
            }
            while ((line = reader.readLine()) != null) {
                simulator.addData(disassembler.parseData(line));
            }
            reader.close();

            simulator.printInstr(_disassembleFilePath);
            simulator.exec();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Simulator {
    List<Instruction> instrList = new ArrayList<>();
    List<Data> dataList = new ArrayList<>();
    int startAddress = 128;
    int position = startAddress;
    int cycle = 1;
    int endAddress = 0;
    int dataAddress = endAddress + 4;
    int R[] = new int[32];
    boolean isBreak = false;

    public Simulator() {
        int i = 0;
        for (; i < R.length; i++) {
            R[i] = 0;
        }
    }

    public void addInstr(Instruction instr) {
        this.instrList.add(instr);
    }

    public void addData(Data data) {
        this.dataList.add(data);
    }

    public void printInstr(String outputFilePath) {
        try {
            FileWriter writer = new FileWriter(new File(outputFilePath));
            int position = startAddress;
            for (Instruction i : instrList) {
                writer.write(i.getContent() + "\t");
                writer.write(String.valueOf(position) + "\t");
                writer.write(i.getPrintValue() + "\n");
                position += 4;
            }
            for (Data data : dataList) {
                writer.write(data.getContent() + "\t");
                writer.write(position + "\t");
                writer.write(data.getValue() + "\n");
                position += 4;
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exec() {
        endAddress = startAddress + instrList.size() * 4;


        while (!isBreak && position <= endAddress) {
            Instruction instruction = getInstrByAddress(position);
            String name = instruction.getName();
            try {
                Method method = this.getClass().getDeclaredMethod(name, Instruction.class);
                method.invoke(this, instruction);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            printCycleResult();
            position += 4;
            cycle++;
        }
    }

    public void saveToMemory(int value, int address) {
        dataList.add((address - dataAddress) / 4, new Data(value));
    }

    public int getMemoryData(int address) {

        return 0;
    }

    // start category 1 -------------------
    public void J(Instruction instruction) {
        int targetAddr = instruction.getArgs()[0];
        this.position = targetAddr;

    }

    public void BEQ(Instruction instruction) {
        int[] args = instruction.getArgs();
        if (args[0] == args[1]) {
            position = args[2];
        }
    }

    public void BGTZ(Instruction instruction) {
        int[] args = instruction.getArgs();
        if (args[0] > 0) {
            position = args[1];
        }
    }

    public void BREAK(Instruction instruction) {
        this.isBreak = true;
    }

    public void SW(Instruction instruction) {
        int[] args = instruction.getArgs();
        saveToMemory(R[args[1]], R[args[0]] + args[2]);
    }

    public void LW(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[1]] = getMemoryData(R[args[0]] + args[2]);
    }

    // end category 1 -------------------


    public void ADD(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] + R[args[2]];
    }

    public void ADDI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] + args[2];
    }

    public void SUB(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] - R[args[2]];
    }

    public void MUL(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] * R[args[2]];
    }

    public void AND(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] & R[args[2]];
    }

    public void ANDI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] & args[2];
    }

    public void OR(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] | R[args[2]];
    }

    public void ORI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] | args[2];
    }

    public void XOR(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] ^ R[args[2]];
    }

    public void XORI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] ^ args[2];
    }

    public void NOR(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = ~(R[args[1]] | R[args[2]]);
    }


    public Instruction getInstrByAddress(int address) {
        int i = (address - startAddress) / 4;
        return instrList.get(i);
    }

    public void printCycleResult() {
        String hyphen = "--------------------";
        try {
            FileWriter writer = new FileWriter(new File(MIPSsim._simulationFilePath));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Data {
    int value;
    String content;

    public Data() {
    }

    public Data(int value) {
        this.value = value;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}

class Disassembler {
    private static final String category1Pattern = "000(?<opcode>\\d{3})\\d{26}";
    private static final String category2Pattern = "110(?<rs>\\d{5})(?<rt>\\d{5})(?<opcode>\\d{3})(?<rd>\\d{5})\\d{11}";
    private static final String category3Pattern = "111(?<rs>\\d{5})(?<rt>\\d{5})(?<opcode>\\d{3})(?<imValue>\\d{16})";

    public Disassembler() {
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

    public Data parseData(String line) {
        Data data = new Data();
        data.setContent(line);
        int symbolFlag = 0;
        symbolFlag = line.charAt(0) - '0';
        line = line.substring(1);

        char[] array = line.toCharArray();
        int num = 0;
        for (int i = 0; i != array.length; i++) {
            int bit = (array[i] - '0') << (array.length - 1 - i);
            num = (num | bit);
        }
        data.setValue((symbolFlag << 31) | num);
        return data;
    }

    private static void processCategory1(Instruction instruction) {
        Pattern pattern = Pattern.compile(category1Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            String opcode = matcher.group("opcode");
            InstrHandler handler = new Category1InstrHandler(opcode);
            handler.handle(instruction);
        }
    }

    private static void processCategory2(Instruction instruction) {
        Pattern pattern = Pattern.compile(category2Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            InstrHandler handler = new Category2InstrHandler(matcher);
            handler.handle(instruction);
        }
    }

    private static void processCategory3(Instruction instruction) {
        Pattern pattern = Pattern.compile(category3Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            InstrHandler handler = new Category3InstrHandler(matcher);
            handler.handle(instruction);
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
    public Integer parseInt(String s) {
        return Integer.parseInt(s);
    }

    public abstract void handle(Instruction instruction);

    public Integer binToDec(String binary) {
        return Integer.valueOf(binary, 2);
    }
}

class Category1InstrHandler extends InstrHandler {
    private Map<String, String[]> map = new HashMap<>();
    private String opcode;
    private Matcher matcher;
    private Instruction instruction;

    public Category1InstrHandler(String opcode) {
        map.put("000", new String[]{"J", "000000(?<instrIndex>\\d{26})"});
        map.put("010", new String[]{"BEQ", "000010(?<rs>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"});
        map.put("100", new String[]{"BGTZ", "000100(?<rs>\\d{5})\\d{5}(?<offset>\\d{16})"});
        map.put("101", new String[]{"BREAK", "\\d{32}"});
        map.put("110", new String[]{"SW", "000110(?<base>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"});
        map.put("111", new String[]{"LW", "000111(?<base>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"});

        this.opcode = opcode;
    }

    @Override
    public void handle(Instruction instruction) {

        this.instruction = instruction;
        String name = map.get(opcode)[0];
        instruction.setName(name);

        Pattern pattern = Pattern.compile(map.get(opcode)[1]);
        matcher = pattern.matcher(instruction.getContent());

        if (matcher.matches()) {
            try {
                Method method = this.getClass().getDeclaredMethod(name);
                method.invoke(this);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void J() {
        String instrIndex = matcher.group("instrIndex");
        Integer target = (binToDec(instrIndex)) << 2;
        instruction.setPrintValue(instruction.getName() + " #" + target);
        instruction.setArgs(new int[]{target});
    }

    public void BEQ() {
        String rs = matcher.group("rs");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        Integer target = (binToDec(offset)) << 2;
        String printValue = instruction.getName() + " R" + binToDec(rs) + ", R" + binToDec(rt) + ", #" + target;
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(rs), binToDec(rt), target});
    }

    public void BGTZ() {
        String rs = matcher.group("rs");
        String offset = matcher.group("offset");
        Integer target = (binToDec(offset)) << 2;
        String printValue = instruction.getName() + " R" + binToDec(rs) + ", #" + target;
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(rs), target});
    }

    public void BREAK() {
        instruction.setPrintValue("BREAK");
    }

    public void SW() {
        String base = matcher.group("base");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        String printValue = instruction.getName() + " R" + binToDec(rt) + ", " + binToDec(offset) + "(R" + binToDec(base) + ")";
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(base), binToDec(rt), binToDec(offset)});
    }

    public void LW() {
        String base = matcher.group("base");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        String printValue = instruction.getName() + " R" + binToDec(rt) + ", " + binToDec(offset) + "(R" + binToDec(base) + ")";
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(base), binToDec(rt), binToDec(offset)});
    }
}

class Category2InstrHandler extends InstrHandler {
    private Map<String, String> map = new HashMap<>();
    private Matcher matcher;

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
    public void handle(Instruction instruction) {
        String opcode = this.matcher.group("opcode");
        String rs = this.matcher.group("rs");
        String rt = this.matcher.group("rt");
        String rd = this.matcher.group("rd");
        String name = map.get(opcode);
        String printValue = name + " R" + binToDec(rd) + ", R" + binToDec(rs) + ", R" + binToDec(rt);

        instruction.setPrintValue(printValue);
        instruction.setName(name);
        instruction.setArgs(new int[]{binToDec(rd), binToDec(rs), binToDec(rt)});
    }
}

class Category3InstrHandler extends InstrHandler {
    private Map<String, String> map = new HashMap<>();
    private Matcher matcher;

    public Category3InstrHandler(Matcher matcher) {
        super();
        this.matcher = matcher;
        map.put("000", "ADDI");
        map.put("001", "ANDI");
        map.put("010", "ORI");
        map.put("011", "XORI");
    }

    @Override
    public void handle(Instruction instruction) {
        String opcode = this.matcher.group("opcode");
        String rs = this.matcher.group("rs");
        String rt = this.matcher.group("rt");
        String imValue = this.matcher.group("imValue");
        String name = map.get(opcode);
        String printValue = name + " R" + binToDec(rt) + ", R" + binToDec(rs) + ", #" + binToDec(imValue);

        instruction.setPrintValue(printValue);
        instruction.setName(name);
        instruction.setArgs(new int[]{binToDec(rt), binToDec(rs), binToDec(imValue)});
    }
}



