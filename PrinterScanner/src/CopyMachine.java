public class CopyMachine {
	Printer printer = new Printer();
	Scanner scanner = new Scanner();

	public void get() throws InterruptedException {
		printer.get();
		scanner.get();
	}

	public void copy() {
		scanner.scan();
		printer.print();
	}

	public void put() {
		printer.put();
		scanner.put();
	}
}
