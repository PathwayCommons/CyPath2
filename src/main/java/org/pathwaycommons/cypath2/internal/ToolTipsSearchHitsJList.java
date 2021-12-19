package org.pathwaycommons.cypath2.internal;

import cpath.client.query.SearchHit;
import cpath.client.query.SearchResponse;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

class ToolTipsSearchHitsJList extends JList
  implements Observer {

  public ToolTipsSearchHitsJList() {
    super(new DefaultListModel());
  }

  @Override
  public String getToolTipText(MouseEvent mouseEvent) {
    int index = locationToIndex(mouseEvent.getPoint());
    if (index >= 0 && getModel() != null) {
      SearchHit record = (SearchHit) getModel().getElementAt(index);
      StringBuilder html = new StringBuilder();
      html.append("<html><table cellpadding=10><tr><td>");
      html.append("<B>").append(record.getBiopaxClass());
      if (!record.getDataSource().isEmpty())
        html.append("&nbsp;").append(
          record.getDataSource().toString());
      if (!record.getOrganism().isEmpty())
        html.append("&nbsp;").append(
          record.getOrganism().toString());
      html.append("</B>&nbsp;");
      html.append("</td></tr></table></html>");
      return html.toString();
    } else {
      return null;
    }
  }

  @Override
  public synchronized void update(Observable o, Object arg) {
    SearchResponse resp = (SearchResponse) arg;
    DefaultListModel lm = (DefaultListModel) this.getModel();
    lm.clear();
    for (SearchHit searchHit : resp.getSearchHit())
      lm.addElement(searchHit);
    setSelectedIndex(0);
  }


  @Override
  public synchronized ListModel getModel() {
    return super.getModel();
  }
}