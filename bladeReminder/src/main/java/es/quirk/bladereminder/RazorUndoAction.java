package es.quirk.bladereminder;
import java.util.List;
import java.util.ArrayList;

public class RazorUndoAction {

	private final int mID;
	private final String mName;
	private final List<Integer> mModifiedRows = new ArrayList<Integer>();


	public RazorUndoAction(int id, String name) {
		mName = name;
		mID = id;
	}

	public void addShave(int id) {
		// previously shave.id was this one
		mModifiedRows.add(id);
	}

	public String getName() {
		return mName;
	}

	public int getID() {
		return mID;
	}

	public List<Integer> getModifiedShaves() {
		return mModifiedRows;
	}
}
