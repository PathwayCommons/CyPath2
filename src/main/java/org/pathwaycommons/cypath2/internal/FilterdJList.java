package org.cytoscape.cpathsquared.internal;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class FilterdJList<T> extends JList {
	private final FilterField<T> filterField;
	private final int DEFAULT_FIELD_WIDTH = 20;
	
	public FilterdJList() {
		setModel(new FilterListModel<T>());
		filterField = new FilterField<T>(DEFAULT_FIELD_WIDTH);
		filterField.setToolTipText("Type something to see only items matching that.");
	}
	   	
	public void setModel(ListModel m) {
		if(m instanceof FilterListModel)
			super.setModel(m);
		else 
			throw new IllegalArgumentException("is not a FilterListMode.");
	}  	
	
	public JTextField getFilterField() {
		return filterField;
	}
	
	class FilterField<E> extends JTextField implements DocumentListener {

		public FilterField(int width) {
			super(width);
			getDocument().addDocumentListener(this);
		}
		
		@Override
		public void changedUpdate(DocumentEvent arg0) {
			((FilterListModel<E>)getModel()).refilter();
		}

		@Override
		public void insertUpdate(DocumentEvent arg0) {
			((FilterListModel<E>)getModel()).refilter();
		}

		@Override
		public void removeUpdate(DocumentEvent arg0) {
			((FilterListModel<E>)getModel()).refilter();
		}	
	}
	    	
	class FilterListModel<E> extends DefaultListModel {
		ArrayList<E> items;
		ArrayList<E> filterItems;

		private synchronized List<E> items() {
			return items;
		}
		
		private synchronized List<E> filterItems() {
			return filterItems;
		}
		
		public FilterListModel() {
			items = new ArrayList<E>();
			filterItems = new ArrayList<E>();
		}

		@Override
		public Object getElementAt(int index) {
			if (index < filterItems().size())
				return filterItems().get(index);
			else
				return null;
		}

		@Override
		public int getSize() {
			return filterItems().size();
		}
		
		@Override
		public void addElement(Object o) {
			items().add((E) o);
			refilter();
		}

		@Override
		public void setElementAt(Object obj, int index) {
			throw new UnsupportedOperationException();
		}
		
		private void refilter() {
			filterItems().clear();
			String term = getFilterField().getText();
			for (E it : items())
				if (it.toString().indexOf(term, 0) != -1)
					filterItems().add(it);
			fireContentsChanged(this, 0, getSize());
		}
		
		@Override
		public void clear() {
			super.clear();
			items().clear();
			refilter();
		}
	}
}