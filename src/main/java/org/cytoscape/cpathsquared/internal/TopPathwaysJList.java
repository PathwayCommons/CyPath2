package org.cytoscape.cpathsquared.internal;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ListModel;

import org.cytoscape.cpathsquared.internal.FilterdJList.FilterListModel;

import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

class TopPathwaysJList extends FilterdJList<SearchHit> implements Observer {
	
	@Override
	public String getToolTipText(MouseEvent mouseEvent) {
		int index = locationToIndex(mouseEvent.getPoint());
		if (-1 < index) {
			SearchHit record = (SearchHit) getModel().getElementAt(index);
			StringBuilder html = new StringBuilder();
			html.append("<html><table cellpadding=10><tr><td>");
			html.append("<B>");
			if(!record.getDataSource().isEmpty())
				html.append("&nbsp;").append(record.getDataSource().toString());
			if(!record.getOrganism().isEmpty())
				html.append("&nbsp;").append(record.getOrganism().toString());
			html.append("</B>&nbsp;");
			html.append("</td></tr></table></html>");
			return html.toString();
		} else {
			return null;
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		SearchResponse resp = (SearchResponse) arg;
		FilterListModel<SearchHit> lm = (FilterListModel<SearchHit>) this.getModel();
		lm.clear();
		List<SearchHit> searchHits = resp.getSearchHit();
		if (!searchHits.isEmpty())
			for (SearchHit searchHit : searchHits)
				lm.addElement(searchHit);	
	}
	
	@Override
	public synchronized ListModel getModel() {
		return super.getModel();
	}
}