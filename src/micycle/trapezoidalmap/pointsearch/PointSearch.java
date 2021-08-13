package micycle.trapezoidalmap.pointsearch;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import micycle.trapezoidalmap.gui.DisplayPanel;
import micycle.trapezoidalmap.gui.DrawSegments;
import micycle.trapezoidalmap.data.TrapezoidalMap;
import micycle.trapezoidalmap.data.Segment;

/**
 * @author Tyler Chenhall
 */
public class PointSearch {

	/**
	 * @param args the command line arguments
	 */
	@SuppressWarnings("empty-statement")
	public static void main(String[] args) {
		final JFrame f = new JFrame();
		DrawSegments ds = new DrawSegments();
		f.add(ds);

		// mimicked the code from:
		// http://stackoverflow.com/questions/9093448/do-something-when-the-close-button-is-clicked-on-a-jframe
		// except I am only making the frame invisible
		f.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				f.setVisible(false);
			}
		});
		// f.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		f.setSize(1200, 820);
		f.setLocationRelativeTo(null);
		f.setTitle("Computation Geometry - Segment Array Builder");
		f.setVisible(true);
		f.setResizable(false);

		// java keeps ignoring this loop if it is empty so we'll just sleep for a bit
		while (f.isVisible()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException ex) {
				System.err.println("Sleep error in PointSearch Class");
			}
		}
		// get the segments once the panel has completed
		Segment[] arr = ds.getSegments();
		System.out.println("got the segments! " + arr.length);
//            storeLines(args[1], arr);
		TrapezoidalMap ss = new TrapezoidalMap(arr);

		JFrame f2 = new JFrame();
		// send the list to the DisplayPanel
		DisplayPanel dp = new DisplayPanel(arr, ss);
		f2.add(dp);
		f2.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// set the size of the window in pixels
		// set the window location
		// set visible
		// set resizable

		// maybe do something else later, but for now:
		f2.setSize(1200, 820);
		f2.setLocationRelativeTo(null);
		f2.setTitle("Computation Geometry - Point Location");
		f2.setVisible(true);
		f2.setResizable(false);
	}
}
