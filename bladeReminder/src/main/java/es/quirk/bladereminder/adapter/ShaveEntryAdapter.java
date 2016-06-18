package es.quirk.bladereminder.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.frankzhu.recyclerviewdemo.adapter.BaseAbstractRecycleCursorAdapter;

import es.quirk.bladereminder.R;
import es.quirk.bladereminder.ShaveEntry;
import es.quirk.bladereminder.database.DataSource;
import es.quirk.bladereminder.widgets.DateLabel;
import es.quirk.bladereminder.widgets.UsesView;

public class ShaveEntryAdapter extends BaseAbstractRecycleCursorAdapter<RecyclerView.ViewHolder> {

	private final LayoutInflater mLayoutInflater;
	private final IClickListener mClickListener;
	private int mSelectedPosition = 0;

	public ShaveEntryAdapter(Context context, IClickListener clickListener) {
		super(context, null);
		mLayoutInflater = LayoutInflater.from(context);
		mClickListener = clickListener;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (position > 0) {
			// this ends up calling the (holder, Cursor, position) version
			super.onBindViewHolder(holder, position - 1);
		} else {
			// do stuff with header here
		}
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, Cursor cursor, int position) {
		if (holder instanceof NormalTextViewHolder) {
			ShaveEntry entry = ShaveEntry.fromCursor(cursor);
			holder.itemView.setSelected(mSelectedPosition == position);
			((NormalTextViewHolder) holder).fillFrom(entry);
		}
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (viewType == 0) {
			return new HeaderViewHolder(mLayoutInflater.inflate(R.layout.view_list_item_header, parent, false));
		} else {
			return new NormalTextViewHolder(mLayoutInflater.inflate(R.layout.shaveentry, parent, false));
		}
	}

	@Override
	public int getItemViewType(int position) {
		return position;
	}

	public interface IClickListener {
		public void onItemClick(long id);
		public int getRazorId();
	}

	public class NormalTextViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.date_label) DateLabel mDateLabel;
		@BindView(R.id.count_label) UsesView mCountLabel;
		@BindView(R.id.comment) TextView mComment;
		long mShaveId;

		NormalTextViewHolder(View view) {
			super(view);
			ButterKnife.bind(this, view);
		}

		public void fillFrom(ShaveEntry entry) {
			int count = entry.getCount();
			mDateLabel.setText(entry.getDate());
			mCountLabel.setText(Integer.toString(count));
			mComment.setText(entry.getComment());
			mShaveId = entry.getID();

			int razorId = mClickListener.getRazorId();
			try {
				razorId = Integer.parseInt(entry.getRazor());
			} catch (NumberFormatException ex) {
			}
			boolean isEnabled = count == 0 || razorId == mClickListener.getRazorId();
			mDateLabel.setEnabled(isEnabled);
			mCountLabel.setEnabled(isEnabled);
			mComment.setEnabled(isEnabled);
		}

		@OnClick(R.id.linear_container)
		void onItemClick() {
			mSelectedPosition = getLayoutPosition();
			notifyItemChanged(mSelectedPosition);
			mClickListener.onItemClick(mShaveId);
		}
	}

	public class HeaderViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.column_header1) View mDateLabel;
		@BindView(R.id.column_header2) View mCountLabel;
		@BindView(R.id.column_header3) View mComment;

		HeaderViewHolder(View view) {
			super(view);
			ButterKnife.bind(this, view);
		}
	}
}
