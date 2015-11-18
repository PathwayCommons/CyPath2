package org.pathwaycommons.cypath2.internal;

import java.net.URL;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

//import org.cytoscape.biopax.internal.util.CytoscapeWrapper;
import org.cytoscape.util.swing.OpenBrowser;


/**
 * Launches the User's External Web Browser.
 *
 * @author Ethan Cerami.
 */
public class LaunchExternalBrowser implements HyperlinkListener {

	private OpenBrowser browser;

	public LaunchExternalBrowser(OpenBrowser browser) {
		this.browser = browser;
	}
	
	/**
	 * User has clicked on a HyperLink.
	 *
	 * @param evt HyperLink Event Object.
	 */
	public void hyperlinkUpdate(HyperlinkEvent evt) {
		URL url = evt.getURL();

		if (url != null) {
			if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				//CytoscapeWrapper.setStatusBarMsg(url.toString());
			} else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
				//CytoscapeWrapper.clearStatusBar();
			} else if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				browser.openURL(url.toString());
			}
		}
	}
}
