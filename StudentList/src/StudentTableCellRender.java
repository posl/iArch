import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public class StudentTableCellRender extends DefaultTableCellRenderer {
	private static final long serialVersionUID = -1831056166440531611L;



	public StudentTableCellRender() {
		super();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		// TODO Auto-generated method stub
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);
		if((boolean)table.getModel().getValueAt(row, 2)){
			this.setBackground(Color.ORANGE);
		}else{
			this.setBackground(Color.WHITE);
		}
		this.setForeground(Color.BLACK);

		return this;
	}

}
