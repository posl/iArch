import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;


public class StudentController {
	public static void filterStudent(JTable table){
		final TableRowSorter<? extends TableModel> sorter = new TableRowSorter<>(table.getModel());
		final RowFilter<TableModel, Integer> filter = new RowFilter<TableModel, Integer>(){
			int counter = 0;
			@Override
			public boolean include(
					javax.swing.RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
				TableModel model = entry.getModel();
				boolean hasClass = (boolean) model.getValueAt(counter, 2);
				counter++;
				return hasClass;
			}
		};
		sorter.setRowFilter(filter);
		table.setRowSorter(sorter);
	}
}
