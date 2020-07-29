package labop.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import labop.config.ConfigParser;

/**
 * CSVWatcher
 */
public class CSVWatcher {
    public final TreeMap<File, LinkedList<LinkedList<String>>> csvs;
    public final ConfigParser settings;

    /*
     * Si usa este constructor tenga cuidado con la función de consumo ya que en
     * este momento el csv no está balanceado por lo que una mala configuración o
     * una petición erronea a alguna columna provocará una excepción
     */
    public CSVWatcher(ConfigParser configs, Consumer<LinkedList<String>> doByLine, File... csvs) {
        settings = configs;
        this.csvs = new TreeMap<>();
        parseAll(doByLine, csvs);
    }

    public CSVWatcher(ConfigParser configs, File... csvs) {
        this(configs, null, csvs);
    }

    public CSVWatcher(ConfigParser config, File file, int width, int height) {
        this.settings = config;
        this.csvs = new TreeMap<>();
        LinkedList<LinkedList<String>> aux = new LinkedList<>();
        LinkedList<String> aux2;
        for (int i = 0; i < height; i++) {
            aux2 = new LinkedList<String>();
            for (int ii=0; ii<width; ii++) {
                aux2.add("");
            }
            aux.add(aux2);
        }
        this.csvs.put(file, aux);
    }

    private void parseAll(Consumer<LinkedList<String>> doByLine, File... files) {
        for (File f : files) {
            csvs.put(f, toLists(f, doByLine));
        }
    }

    private LinkedList<String> parserLine(String line, AtomicInteger max) {
        final Scanner scan = new Scanner(line);
        String current;
        boolean stringEnv = false;
        final StringBuffer buff = new StringBuffer();
        final LinkedList<String> words = new LinkedList<>();
        scan.useDelimiter("");
        while (scan.hasNext()) {
            current = scan.next();
            if (current.equals("\"")) {
                buff.append("\"");
                stringEnv = !stringEnv;
            } else if (stringEnv) {
                // ignoramos strings
                buff.append(current);
            } else if (current.equals(",")) {
                words.add(buff.toString());
                buff.delete(0, buff.length());
            } else {
                buff.append(current);
            }
        }

        if (buff.length() > 0) {
            words.add(buff.toString());
        }

        scan.close();
        if (words.size() > max.get()) {
            max.set(words.size());
        }
        return words;
    }

    private LinkedList<LinkedList<String>> toLists(File file, Consumer<LinkedList<String>> doByLine) {
        LinkedList<LinkedList<String>> result = new LinkedList<>();
        LinkedList<Integer> cols = settings.getAllCols();
        AtomicInteger max = new AtomicInteger(cols.stream().max(Integer::compare).orElse(0));
        try {
            FileInputStream in = new FileInputStream(file);
            StringBuffer buff = new StringBuffer();
            int aux;

            /* Lecturas */
            if (doByLine == null) {
                while ((aux = in.read()) != -1) {
                    if ((char) aux == '\n') {
                        result.add(parserLine(buff.toString(), max));
                        buff.delete(0, buff.length());
                    } else {
                        buff.append((char) aux);
                    }
                }
            } else {
                int rowN = 0;
                LinkedList<String> lineRow;
                while ((aux = in.read()) != -1) {
                    if ((char) aux == '\n') {
                        rowN++;
                        lineRow = parserLine(buff.toString(), max);
                        if (rowN >= settings.get("startline", Integer.class)) {
                            doByLine.accept(lineRow);
                        }
                        result.add(lineRow);
                        buff.delete(0, buff.length());
                    } else {
                        buff.append((char) aux);
                    }
                }
            }
            /* Fin Lecturas */

            in.close();
            if (buff.length() != 0) {
                result.add(parserLine(buff.toString(), max));
                buff.delete(0, buff.length());
            }
            for (int i = settings.get("startline", Integer.class) - 1; i < result.size(); i++) {
                for (int ii = result.get(i).size(); ii < max.get(); ii++) {
                    result.get(i).add("");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private byte[] toCSV(LinkedList<LinkedList<String>> inf) throws UnsupportedEncodingException {
        StringBuffer buff = new StringBuffer();
        String aux;
        for(LinkedList<String> list : inf){
            aux = list.stream().reduce((a,b)->a+","+b).orElse("");
            buff.append(aux);
            /* if(!aux.trim().isEmpty()) {
                buff.append(",");
            } */
            buff.append("\n");
        }
        if(buff.length()>0)
            buff.deleteCharAt(buff.length()-1);
        return buff.toString().getBytes("ISO-8859-1");
    }

    public LinkedList<String> search(String colN, String key){
        int col = settings.get(colN, Integer.class)-1;
        int start = settings.get("startline", Integer.class)-1;
        LinkedList<String> row;
        for(LinkedList<LinkedList<String>> csv : csvs.values()){
            for(int i = start; i < csv.size(); i++){
                row = csv.get(i);
                if(row.size()>col && row.get(col).replaceAll("\"", "").replaceAll("'", "").trim().equals(key.replaceAll("\"", "").replaceAll("'", "").trim())){
                    return row;
                }
            }
        }
        return null;
    }

    public void consumeCSVs(Consumer<LinkedList<String>> foo){
        int start = settings.get("startline", Integer.class)-1;
        for(LinkedList<LinkedList<String>> csv : csvs.values()){
            for(int i = start; i < csv.size(); i++){
                foo.accept(csv.get(i));
            }
        }
    }

    public String writeAndGet(TreeMap<String,String> data, String colKeyname, String key, String colName){
        LinkedList<String> result = search(colKeyname, key);
        if(result == null) return null;
        int col;
        for(String k : data.keySet()){
            col = settings.get(k, Integer.class)-1;
            result.remove(col);
            result.add(col, data.get(k));
        }
        return result.get(settings.get(colName, Integer.class)-1);
    }

    public void writeRow(int num, LinkedList<String> row) {
        LinkedList<LinkedList<String>> table = csvs.firstEntry().getValue();
        LinkedList<String> rowtable = table.get(num-1);
        for(int i = 0; i < row.size() && i < rowtable.size(); i++) {
            rowtable.set(i, row.get(i));
        }
    }

    public void rewrite() {
        csvs.forEach((file, content)->{
            try {
                if(file.exists()) file.delete();
                FileOutputStream stream = new FileOutputStream(file);
                stream.write(toCSV(content));
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } 
        });
    }

}