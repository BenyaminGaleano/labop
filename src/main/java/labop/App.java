/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package labop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Scanner;
import java.util.stream.Collectors;

import labop.config.ConfigParser;
import labop.csv.CSVWatcher;
import labop.display.Display;

public class App {

	private Display display;
	private Scanner scan;
	private AppMode mode;
	private LogBuilder logBuilder;

	private App(Display display, Scanner scan, AppMode mode) {
		this.display = display;
		this.scan = scan;
		this.mode = mode;

	}

	private void recoveryLinear(File log, LinkedList<Section> trace) {
		try {
			Scanner reader = new Scanner(log);
			// comentario <#> punteo
			String aux = "";
			reader.nextLine();

			for (Section section : trace) {
				if (!reader.hasNext())
					break;
				aux = reader.nextLine();
				if (aux.isEmpty())
					break;
				section.set(Integer.parseInt(aux.split("<#>")[1].trim()), aux.split("<#>")[0].trim());
			}
			reader.close();
		} catch (FileNotFoundException e) {
			display.error("no se pudo leer el log");
		}
	}

	private void writeLog(File log) {
		try {
			FileOutputStream out = new FileOutputStream(log);
			out.write(logBuilder.tobytes());
			out.close();
		} catch (IOException e) {
			display.error("no se pudo escribir LOG");
		}
	}

	private void linear(CSVWatcher input, CSVWatcher notas, CSVWatcher comentarios, LinkedList<Section> trace) {
		final ConfigParser inconf = input.settings;
		final ConfigParser outconf = notas.settings;
		AtomicInteger count = new AtomicInteger();
		File log = new File("input/log.log");
		AtomicInteger start = new AtomicInteger();
		AtomicBoolean close = new AtomicBoolean();

		logBuilder = new LogBuilder(0, trace);

		if (log.exists()) {
			recoveryLinear(log, trace);
			try {
				Scanner reader = new Scanner(log);
				start.set(reader.nextInt());
				reader.close();
			} catch (FileNotFoundException e) {
				display.error("No se pudo recuperar el estado :c");
			}
		}

		input.consumeCSVs((row) -> {
			if (close.get())
				return;
			if (count.incrementAndGet() < start.get())
				return;
			
			logBuilder.studentLine = count.get();
			writeLog(log);

			display.show("\n\nAlumno: " + row.get(inconf.get("idCol", Integer.class) - 1) + "\n", Display.YELLOW);

			Section section;
			int prev = 0;
			for (int i = 0; i < trace.size(); i++) {
				if (trace.get(i).fill())
					prev = i + 1;
			}

			process: for (int i = prev; i < trace.size(); i++) {

				section = trace.get(i);

				display.show("\nEjercicio: " + section.name + "\nvalor: " + section.points + "\n", Display.YELLOW);

				String command;

				readcommand: while (true) {
					display.show("\n# ", Display.BLUE);
					command = scan.nextLine();
					// comandos

					switch (command.trim().split(" ")[0]) {
						case "close":
							close.set(true);
							display.msg("Hasta luego " + System.getProperty("user.name"));
							return;

						case "prev":
							int aux00 = i;
							i = prev - 1;
							prev = aux00;
							continue process;

						case "next":
							prev = i;
							if (i+1 == trace.size()) {
								prev = 0;
							} 
							continue process;

						case "exit":
							return;

						case "goto":
							Scanner cmdscan = new Scanner(command);
							int aux01 = i;
							cmdscan.next();
							String sect00 = cmdscan.nextLine().trim();
							for (int j = 0; j < trace.size(); j++) {
								if (trace.get(j).name.toLowerCase().trim().equals(sect00.toLowerCase())) {
									prev = i;
									i = j - 1;
								}
							}
							if (i + 1 == aux01) {
								display.warning("No se encontró la sección que pidió");
								continue readcommand;
							}
							cmdscan.close();
							continue process;

						case "finish":
							if (outconf.get("fill", Boolean.class)) {
								String comentario = outconf.get("comment", String.class);
								LinkedList<LinkedList<String>> nots = notas.csvs.firstEntry().getValue();
								LinkedList<LinkedList<String>> comentaris = comentarios.csvs.firstEntry().getValue();
								for (int j = 0; j < nots.size(); j++) {
									if (nots.get(j).stream().reduce((a,b) -> a+b).orElseGet(() -> "").isEmpty()) {
										for (int k = 0; k < nots.get(j).size(); k++) {
											nots.get(j).set(k, "0");
										}
										comentaris.get(j).set(0, comentario);
									}
								}
								notas.rewrite();
								comentarios.rewrite();
							}
							log.delete();
							close.set(true);
							display.msg("Bien hecho " + System.getProperty("user.name") + " terminaste :3");
							return;

						case "set":
							String content = command.trim().split(" ")[1];
							int nota = 0;
							if (content.toLowerCase().equals("max")) {
								notas.writeRow(count.get(), trace.stream().map(sec -> {
									return sec.points + "";
								}).collect(Collectors.toCollection(LinkedList::new)));
							} else {
								try {
									nota = Integer.parseInt(content);
								} catch (Exception e){
									display.error("nota no válida");
									continue readcommand;
								}
								final int nota00 =  nota;
								notas.writeRow(count.get(), trace.stream().map(sec -> {
									return nota00 + "";
								}).collect(Collectors.toCollection(LinkedList::new)));
							}

							display.show("// ", Display.BLUE);
							String aux02 = scan.nextLine();

							String commto = "\"" + aux02.trim() + "\"";
							comentarios.writeRow(count.get(), new LinkedList<String>() {
								private static final long serialVersionUID = 1L;
								{
									add(commto);
								}
							});
							notas.rewrite();
							comentarios.rewrite();
							return;

						case "":
							prev = i;
							break readcommand;

						default:
							display.error("comando inválido");
							continue readcommand;
					}
				}

				// calificar
				display.show("// ", Display.BLUE);
				String comentario = scan.nextLine();
				display.show("P: ", Display.BLUE);
				int points = scan.nextInt();
				scan.nextLine();
				section.total = points;
				section.comment = comentario;
				writeLog(log);
			}

			notas.writeRow(count.get(), trace.stream().map(sec -> {
				return sec.total + "";
			}).collect(Collectors.toCollection(LinkedList::new)));
			String aux = "";
			for (Section sec : trace) {
				if (sec.comment.trim().isEmpty())
					continue;
				aux += sec.name + ") " + sec.comment + " ";
			}

			String commto = "\"" + aux.trim() + "\"";
			comentarios.writeRow(count.get(), new LinkedList<String>() {
				private static final long serialVersionUID = 1L;
				{
					add(commto);
				}
			});

			display.show("Alumno: " + row.get(inconf.get("idCol", Integer.class) - 1) + " Nota: " + trace.stream().map((sc)-> sc.total).reduce((a,b)->a+b).orElseGet(()->0) + "\n", Display.GREEN);

			notas.rewrite();
			comentarios.rewrite();
			trace.forEach(sec -> {
				sec.reset();
			});
		});
	}

	private void search(CSVWatcher input, CSVWatcher notas, CSVWatcher comentarios, LinkedList<Section> trace) {
		final ConfigParser inconf = input.settings;
		final ConfigParser outconf = notas.settings;
		AtomicInteger count = new AtomicInteger();
		File log = new File("input/log.log");
		AtomicInteger start = new AtomicInteger();
		AtomicBoolean backup = new AtomicBoolean();
		AtomicBoolean close = new AtomicBoolean();
		display.show("\n", Display.BLUE);
		display.inf("el prefijo & indica buscador. Escriba el carnet o enter para ir al primero");

		logBuilder = new LogBuilder(0, trace);

		if (log.exists()) {
			recoveryLinear(log, trace);
			backup.set(true);
			try {
				Scanner reader = new Scanner(log);
				start.set(reader.nextInt());
				reader.close();
			} catch (FileNotFoundException e) {
				display.error("No se pudo recuperar el estado :c");
			}
		}

		LinkedList<String> row;

		search: while(true) {
			if (close.get())
				return;
			
			if (backup.get()) {
				row = input.csvs.firstEntry().getValue().get(inconf.get("startline", Integer.class) - 1 + start.get());
				count.set(start.get());
				backup.set(false);
			} else {
				display.show("\n& ", Display.BLUE);
				String carnet = scan.nextLine();
				if ( !carnet.isEmpty() ) {
					row = input.search("idCol", carnet);
					if (row == null)
						continue search;
					count.set(input.lastvisit -  inconf.get("startline", Integer.class) + 2);
				} else {
					row = input.csvs.firstEntry().getValue().getFirst();
				}
			}
			
			logBuilder.studentLine = count.get();
			writeLog(log);

			display.show("\n\nAlumno: " + row.get(inconf.get("idCol", Integer.class) - 1) + "\n", Display.YELLOW);

			Section section;
			int prev = 0;
			for (int i = 0; i < trace.size(); i++) {
				if (trace.get(i).fill())
					prev = i + 1;
			}

			process: for (int i = prev; i < trace.size(); i++) {

				section = trace.get(i);

				display.show("\nEjercicio: " + section.name + "\nvalor: " + section.points + "\n", Display.YELLOW);

				String command;

				readcommand: while (true) {
					display.show("\n# ", Display.BLUE);
					command = scan.nextLine();
					// comandos

					switch (command.trim().split(" ")[0]) {
						case "close":
							close.set(true);
							display.msg("Hasta luego " + System.getProperty("user.name"));
							break search;

						case "prev":
							int aux00 = i;
							i = prev - 1;
							prev = aux00;
							continue process;

						case "next":
							prev = i;
							if (i+1 == trace.size()) {
								prev = 0;
							} 
							continue process;

						case "exit":
							continue search;

						case "goto":
							Scanner cmdscan = new Scanner(command);
							int aux01 = i;
							cmdscan.next();
							String sect00 = cmdscan.nextLine().trim();
							for (int j = 0; j < trace.size(); j++) {
								if (trace.get(j).name.toLowerCase().trim().equals(sect00.toLowerCase())) {
									prev = i;
									i = j - 1;
								}
							}
							if (i + 1 == aux01) {
								display.warning("No se encontró la sección que pidió");
								continue readcommand;
							}
							cmdscan.close();
							continue process;

						case "finish":
							if (outconf.get("fill", Boolean.class)) {
								String comentario = outconf.get("comment", String.class);
								LinkedList<LinkedList<String>> nots = notas.csvs.firstEntry().getValue();
								LinkedList<LinkedList<String>> comentaris = comentarios.csvs.firstEntry().getValue();
								for (int j = 0; j < nots.size(); j++) {
									if (nots.get(j).stream().reduce((a,b) -> a+b).orElseGet(() -> "").isEmpty()) {
										for (int k = 0; k < nots.get(j).size(); k++) {
											nots.get(j).set(k, "0");
										}
										comentaris.get(j).set(0, comentario);
									}
								}
								notas.rewrite();
								comentarios.rewrite();
							}
							log.delete();
							display.msg("Bien hecho " + System.getProperty("user.name") + " terminaste :3");
							break search;

						case "set":
							String content = command.trim().split(" ")[1];
							int nota = 0;
							if (content.toLowerCase().equals("max")) {
								notas.writeRow(count.get(), trace.stream().map(sec -> {
									return sec.points + "";
								}).collect(Collectors.toCollection(LinkedList::new)));
							} else {
								try {
									nota = Integer.parseInt(content);
								} catch (Exception e){
									display.error("nota no válida");
									continue readcommand;
								}
								final int nota00 =  nota;
								notas.writeRow(count.get(), trace.stream().map(sec -> {
									return nota00 + "";
								}).collect(Collectors.toCollection(LinkedList::new)));
							}

							display.show("// ", Display.BLUE);
							String aux02 = scan.nextLine();

							String commto = "\"" + aux02.trim() + "\"";
							comentarios.writeRow(count.get(), new LinkedList<String>() {
								private static final long serialVersionUID = 1L;
								{
									add(commto);
								}
							});
							notas.rewrite();
							comentarios.rewrite();
							continue search;

						case "":
							prev = i;
							break readcommand;

						default:
							display.error("comando inválido");
							continue readcommand;
					}
				}

				// calificar
				display.show("// ", Display.BLUE);
				String comentario = scan.nextLine();
				display.show("P: ", Display.BLUE);
				int points = scan.nextInt();
				scan.nextLine();
				section.total = points;
				section.comment = comentario;
				writeLog(log);
			}

			notas.writeRow(count.get(), trace.stream().map(sec -> {
				return sec.total + "";
			}).collect(Collectors.toCollection(LinkedList::new)));
			String aux = "";
			for (Section sec : trace) {
				if (sec.comment.trim().isEmpty())
					continue;
				aux += sec.name + ") " + sec.comment + " ";
			}

			String commto = "\"" + aux.trim() + "\"";
			comentarios.writeRow(count.get(), new LinkedList<String>() {
				private static final long serialVersionUID = 1L;
				{
					add(commto);
				}
			});
			
			log.delete();
			display.show("Alumno: " + row.get(inconf.get("idCol", Integer.class) - 1) + " Nota: " + trace.stream().map((sc)-> sc.total).reduce((a,b)->a+b).orElseGet(()->0) + "\n", Display.GREEN);

			notas.rewrite();
			comentarios.rewrite();
			trace.forEach(sec -> {
				sec.reset();
			});
		}
	}

	private void start() {
		File infile = new File("input/");
		File outfile = new File("output/");

		if (!infile.exists())
			infile.mkdir();
		if (!outfile.exists())
			outfile.mkdir();

		ConfigParser inconf = null;
		ConfigParser outconf = null;

		try {
			inconf = new ConfigParser("input.yaml");
			outconf = new ConfigParser("output.yaml");
		} catch (Exception e) {
			display.error("los archivos de configuración son obligatorios");
		}

		CSVWatcher input = new CSVWatcher(inconf, infile.listFiles((file, fname) -> fname.matches(".*\\.csv")));

		File notf = new File("output/notas.csv");
		File comf = new File("output/comentarios.csv");
		CSVWatcher notas = null;
		CSVWatcher comments = null;

		if (!notf.exists()) {
			int width = inconf.get("exercises", Integer.class);
			int height = input.csvs.firstEntry().getValue().size() - (inconf.get("startline", Integer.class) - 1);
			notas = new CSVWatcher(outconf, notf, width, height);
			comments = new CSVWatcher(outconf, comf, 1, height+1);
		} else {
			notas = new CSVWatcher(outconf, notf);
			comments = new CSVWatcher(outconf, comf);
		}

		String delimiter1 = inconf.get("scheme", String.class);
		String delimiter2 = delimiter1.split("\\*")[2].trim();
		delimiter1 = delimiter1.split("\\*")[1].trim();

		LinkedList<Section> trace = new LinkedList<>();

		int countId = 0;
		int endpoint, startpoint;
		String name = null;
		int points = 0;

		ConcurrentLinkedQueue<Integer> mul = new ConcurrentLinkedQueue<>();

		for (String mult : inconf.get("partitions", String.class).split(" ")) {
			mul.add(Integer.parseInt(mult));
		}

		ConcurrentLinkedQueue<String> sections = new ConcurrentLinkedQueue<>();
		for (String celda : input.csvs.firstEntry().getValue().get(inconf.get("startline", Integer.class) - 4)) {
			if (countId++ >= inconf.get("idCol", Integer.class)
					&& countId < inconf.get("idCol", Integer.class) + inconf.get("exercises", Integer.class) + 1) {
				if (!celda.trim().isEmpty()) {
					for (int i = 0, aux = mul.poll(); i < aux; i++) {
						sections.add(celda.trim());
					}
				}
			}
		}

		countId = 0;

		for (String celda : input.csvs.firstEntry().getValue().get(inconf.get("startline", Integer.class) - 2)) {
			if (countId++ >= inconf.get("idCol", Integer.class)
					&& countId < inconf.get("idCol", Integer.class) + inconf.get("exercises", Integer.class) + 1) {
				endpoint = 0;
				startpoint = 0;
				for (; endpoint < celda.length(); endpoint++) {
					if (celda.charAt(endpoint) == delimiter1.charAt(0)) {
						name = sections.poll() + "-" + celda.substring(startpoint, endpoint).trim();
						startpoint = endpoint + 1;
					} else if (celda.charAt(endpoint) == delimiter2.charAt(0)) {
						points = Integer.parseInt(celda.substring(startpoint, endpoint).trim());
					}
				}
				trace.add(new Section(points, name));
			}
		}

		display.inf("el prefijo // es para comentarios");
		display.inf("el prefijo P: es para el puntaje");
		display.inf("el prefijo # indica consola utilice los siguientes comandos:");
		display.show("goto SECCIÓN (indique el nombre a la seccion a la que quiere saltar)\n", Display.GREEN);
		display.show("prev (ir al ejercicio previamente visitado)}\n", Display.GREEN);
		display.show("next (ir al siguiente)\n", Display.GREEN);
		display.show("exit (sale del estudiante y se pierden los datos)\n", Display.GREEN);
		display.show("close (para el programa)\n", Display.GREEN);
		display.show("finish (se terminó de calificar - modo búsqueda\n", Display.GREEN);
		display.show("(presione enter para proseguir)", Display.GREEN);

		switch (mode) {
			case linear:
				linear(input, notas, comments, trace);
				break;

			case search:
				search(input, notas, comments, trace);
				break;
		}
	}

	public static void main(String[] args) {

		/* Creación de display */
		ConfigParser displayconf = null;

		try {
			displayconf = new ConfigParser("display.yaml");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Display display = new Display(displayconf);

		display.show("\nHerramienta auxiliar para calificar\n\n", Display.YELLOW);

		display.msg("Seleccione modo de ejecución.\n\t1. Linear\n\t2. Busqueda");
		display.show("# ", Display.BLUE);

		Scanner scan = new Scanner(System.in);
		String op = "";

		while (!(op = scan.nextLine()).matches("1|2")) {
			display.error("sólo puede ingresar 1 o 2");
			display.show("# ", Display.BLUE);
		}

		AppMode mode = null;
		switch (op) {
			case "1":
				display.msg("Modo Lineal Activo");
				mode = AppMode.linear;
				break;

			case "2":
				display.msg("Modo Búsqueda Activo");
				mode = AppMode.search;
				break;
		}

		App tool = new App(display, scan, mode);
		tool.start();

		scan.close();
	}
}

enum AppMode {
	linear, search
}

class Section {
	final int points;
	final String name;
	int total;
	String comment;

	public Section(int points, String name) {
		this.points = points;
		this.name = name;
		this.total = 0;
		this.comment = "";
	}

	public void reset() {
		this.total = 0;
		this.comment = "";
	}

	public void set(int total, String comment) {
		this.total = total;
		this.comment = comment;
	}

	public boolean fill() {
		return !this.comment.trim().isEmpty() || this.total > 0;
	}

	@Override
	public String toString() {
		return "N: " + name + " P: " + points + " T:" + total;
	}
}

class LogBuilder {
	final LinkedList<Section> trace;
	int studentLine;

	public LogBuilder(int studentLine, LinkedList<Section> trace) {
		this.trace = trace;
		this.studentLine = studentLine;
	}

	public byte[] tobytes() throws UnsupportedEncodingException {
		StringBuilder logBuilder = new StringBuilder();
		logBuilder.append(studentLine);
		logBuilder.append('\n');
		trace.forEach((section) -> {
			logBuilder.append(section.comment + "<#>" + section.total);
			logBuilder.append('\n');
		});
		return logBuilder.toString().getBytes();
	}
}