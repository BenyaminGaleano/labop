package labop.config;

import java.io.File;
import java.io.FileInputStream;

import org.yaml.snakeyaml.*;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class ConfigParser {
    private final LinkedHashMap<String, Object> settings = new LinkedHashMap<>();
    private final LinkedList<String> errors = new LinkedList<>();

    public ConfigParser(final String configPath) throws FileNotFoundException {
        final File file = new File(configPath);
        Yaml parser = new Yaml();
        this.settings.putAll(parser.load(new FileInputStream(file)));
    }

    public <T> T get(final String value, final Class<T> clss){
        if(clss == String.class) {
            return clss.cast(settings.get(value));
        } else if(clss == Double.class) {
            return clss.cast(((Number) settings.get(value)).doubleValue());
        } else if(clss == Integer.class) {
            return clss.cast(((Number) settings.get(value)).intValue());
        } else if(clss == Boolean.class) {
            return clss.cast(settings.get(value));
        }
        return null;
    }

    public LinkedList<Integer> getAllCols(){
        LinkedList<Integer> buff = new LinkedList<>();

        settings
        .keySet()
        .stream()
        .filter((element)->element.contains("Col"))
        .map((key)->settings.get(key))
        .map((element) -> {
            try {
                return (int) element;
            } catch (Exception e) {
               return null;
            }
        })
        .filter((element)->element!=null)
        .forEach(element->buff.add(element));

        return buff;
    }

    public String errors(){
        return errors.stream().reduce((er1,er2)->er1.concat("\n").concat(er2)).orElse("");
    }

    public boolean hasErrors(){
        return errors.size() > 0;
    }

    @Override
    public String toString() {
        return settings.toString();
    }
}

/**
 * ConfigParser
 */
/* public class ConfigParser {
    private final TreeMap<String, String> settings = new TreeMap<>();
    private final LinkedList<String> errors = new LinkedList<>();

    private void appendError(final String inf, final String current, final int line, final String exInfo){
        errors.add(inf+"\n>> "+current+"\nLinea "+line+" "+exInfo);
    }

    private void appendError(final String inf, final String current, final int line){
        appendError(inf, current, line, "");
    }

    public ConfigParser(final String configPath)  {
        final File file = new File(configPath);
        Scanner parser;
        try {
            parser = new Scanner(file);
            final StringBuffer buff = new StringBuffer();
            String aux;
            boolean stringEnv = false, numberEnv = false;
            int line = 0;
            String lastKey = null;
            parser.useDelimiter("");
            while(parser.hasNextLine()){
                aux = parser.nextLine();
                line++;
                for(final String ch:aux.split("")){

                    if(ch.matches(":")) {
                        numberEnv = false;
                        stringEnv = false;
                        lastKey = buff.toString();
                        buff.delete(0, buff.length());
                        if(lastKey.trim().isEmpty()){
                            appendError("Llave vacia", aux, line);
                            lastKey += ""+errors.size();
                        }
                    } else if(ch.matches("\t|\\s")){
                        if(stringEnv){
                            buff.append(ch);
                        } 
                    } else if (!stringEnv&&(ch.matches("\\d")||numberEnv)){
                        numberEnv = true;
                        if(ch.matches("\\d|\\.")){
                            buff.append(ch);
                        } else {
                            appendError(
                                "Un numero no puede contener simbolos diferentes a los digitos",
                                aux,
                                line, 
                                "se desconoce el simbolo "+ch
                            );
                        }
                    } else {
                        if(!ch.equals("\"")){   
                            buff.append(ch);
                        }
                        stringEnv = true;
                    }   
                }
                numberEnv = false;
                stringEnv = false;
                if(lastKey == null){
                    errors.add("Campo sin llave\n"+aux+"\nlinea "+line);
                    lastKey=errors.size()+"";
                    appendError("Campo sin llave", aux, line);
                }
                if(settings.containsKey(lastKey)){
                    appendError("Llave definida anteriormente", aux, line);
                    lastKey = lastKey+errors.size()+"";
                }
                settings.put(lastKey, buff.toString().trim());
                buff.delete(0, buff.length());
                lastKey = null;
            }
            parser.close();
        } catch (FileNotFoundException e) {}
    }

    public <T> T get(final String value, final Class<T> clss){
        if(clss == String.class){
            return clss.cast(settings.get(value));
        } else if(clss == Double.class){
            return clss.cast(Double.parseDouble(settings.get(value)));
        } else if(clss == Integer.class){
            return clss.cast(Integer.parseInt(settings.get(value)));
        }
        return null;
    }

    public LinkedList<Integer> getAllCols(){
        LinkedList<Integer> buff = new LinkedList<>();

        settings
        .keySet()
        .stream()
        .filter((element)->element.contains("Col"))
        .map((key)->settings.get(key))
        .map((element) -> {
            try {
                return Integer.parseInt(element);
            } catch (Exception e) {
               return null;
            }
        })
        .filter((element)->element!=null)
        .forEach(element->buff.add(element));

        return buff;
    }

    public String errors(){
        return errors.stream().reduce((er1,er2)->er1.concat("\n").concat(er2)).orElse("");
    }

    public boolean hasErrors(){
        return errors.size() > 0;
    }

    @Override
    public String toString() {
        return settings.toString();
    }
} */