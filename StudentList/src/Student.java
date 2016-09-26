
public class Student {
	private String id;
	private String name;
	private boolean hasClass;
	private Object tableData[];


	public Student(String id, String name, boolean age) {
		this.id = id;
		this.name = name;
		this.hasClass = age;
		Object[] tmpTableData = {this.id, this.name, this.hasClass};
		this.tableData = tmpTableData;
	}
	public Object[] getTableData(){
		return this.tableData;
	}

}
