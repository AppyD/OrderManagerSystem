package Database;

//TODO figure out how to make this abstract or an interface, but want the method to be static
public interface Database {

	static void write(Object o){
		System.out.println(o.toString());
	}

}