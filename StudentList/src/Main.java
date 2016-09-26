import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class Main extends JFrame implements ActionListener{

	private static final long serialVersionUID = 1L;
	private Student[] studentData = {
			new Student("1","Taro",true),
			new Student("2","Jiro",true),
			new Student("3","Saburo",false),
			new Student("4","Hanako",true),
			new Student("5","Yumi",false),
			new Student("6","Emi",true)
	};
	private String[] columnName = { "ID", "Name", "Class" };
	private Object[][] tableData;
	public static Main mainFrame;

	public Main() {
		setTitle("StudentList");
		setVisible(true);
		setBounds(0, 0, 320, 240);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.tableData = new Object[studentData.length][];
		for(int i = 0; i < studentData.length; i++){
			tableData[i] = studentData[i].getTableData();
		}

		TableModel model = new DefaultTableModel(tableData, columnName){
			private static final long serialVersionUID = -7956029302838217952L;

			@Override
			public boolean isCellEditable(int row, int column) {
				// TODO Auto-generated method stub
				return false;
			}
		};

		JTable table = new JTable(model);

		JButton button = new JButton("授業取得者のみに絞り込む");
		button.addActionListener(this);

		JPanel p = new JPanel();
		p.add(table.getTableHeader(), BorderLayout.PAGE_START);
		p.add(table);
		p.add(button);

		getContentPane().add(p, BorderLayout.CENTER);

	}

	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		mainFrame = new Main();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO 自動生成されたメソッド・スタブ
		Container container = mainFrame.getContentPane();
		JTable table = (JTable) ((JPanel)container.getComponent(0)).getComponent(1);
		// filter Students
		StudentController.filterStudent(table);
	}

}
