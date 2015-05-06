package br.net.shima.utils.caffeinate;
import br.net.shima.utils.caffeinate.CaffeinateRunner;


public class CaffeinateTest {
	public static void main(String[] args) {
		try {
			CaffeinateRunner caffeine = new CaffeinateRunner();
			System.out.println(caffeine.caffeinate(1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
