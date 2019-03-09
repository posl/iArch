import java.util.concurrent.Semaphore;

public class Printer {
	private Semaphore lock = new Semaphore(1);

	public void get() throws InterruptedException {
		lock.acquire();
	}

	public void put() {
		lock.release();
	}

	public void print() {
		//
	}
}
