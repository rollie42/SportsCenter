package com.espn;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ImgTextAdapter extends BaseAdapter implements Filterable {
	private class ItemData {
		ItemData(int imageID, CharSequence text) {
			mImageID = imageID;
			mText = text;
		}
		
		int mImageID;
		CharSequence mText;		
	}
	
	private ArrayList<ItemData> mItemDatas;
	private ArrayList<ItemData> mOrigItemDatas = null;
	private Context mContext;
	public boolean mVertical = false;
	public int mWidth = ListView.LayoutParams.MATCH_PARENT;
	public int mHeight = ListView.LayoutParams.MATCH_PARENT;
	public int mColor = Color.rgb(24, 24, 24);
	private ImgTextFilter mFilter = null;
	private final Object mLock = new Object();
	
	ImgTextAdapter(Context context) {
		mItemDatas = new ArrayList<ItemData>();
		mContext = context;
	}
	
	ImgTextAdapter(Context context, ArrayList<ItemData> itemDatas) {
		mItemDatas = itemDatas;
		mContext = context;
	}
	
	public void add(int imageID, CharSequence text) {
		mItemDatas.add(new ItemData(imageID, text));
	}
	
	@Override
	public int getCount() {
		return mItemDatas.size();
	}

	@Override
	public Object getItem(int position) {
		return mItemDatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout resultView; 
		if (convertView == null || !(convertView instanceof LinearLayout)) {
			// Build the resultView
			resultView = new LinearLayout(mContext);
			resultView.setLayoutParams(new ListView.LayoutParams(mWidth, mHeight));
			
			ImageView imgv = new ImageView(mContext);
			TextView tv = new TextView(mContext);			
			if (mVertical) {
				resultView.setOrientation(LinearLayout.VERTICAL);
				imgv.setLayoutParams(new LinearLayout.LayoutParams(mWidth, mWidth));
				imgv.setPadding(0, 0, 0, 0);
				tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
				tv.setPadding(0, 0, 0, 0);
				tv.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
			}			
			else {
				imgv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
							
				tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));				
				tv.setPadding(10, 0, 10, 0);
				tv.setGravity(Gravity.LEFT);
			}
						
			imgv.setAdjustViewBounds(true);
			imgv.setScaleType(ScaleType.FIT_START);
			imgv.setDuplicateParentStateEnabled(true);

			tv.setTextColor(mColor);			
			tv.setDuplicateParentStateEnabled(true);
			
			resultView.addView(imgv, 0);
			resultView.addView(tv, 1);
		}
		else {
			resultView = (LinearLayout) convertView;
		}
		
		((ImageView)resultView.getChildAt(0)).setImageResource(mItemDatas.get(position).mImageID);
		((TextView)resultView.getChildAt(1)).setText(mItemDatas.get(position).mText);		
		return resultView;
	}

	@Override
	public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ImgTextFilter();
        }
        return mFilter;
    }

    private class ImgTextFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOrigItemDatas == null) {
            	mOrigItemDatas = new ArrayList<ItemData>(mItemDatas);
            }            
            
            if (prefix == null || prefix.length() == 0) {
                synchronized (mLock) {                	
                    results.values = new ArrayList<ItemData>(mOrigItemDatas);;
                    results.count = mOrigItemDatas.size();
                }
            } else {
                String prefixString = prefix.toString().toLowerCase();
                
                final ArrayList<ItemData> newValues = new ArrayList<ItemData>(mOrigItemDatas.size());

                for (int i = 0; i < mOrigItemDatas.size(); i++) {
                    final ItemData value = mOrigItemDatas.get(i);
                    final String valueText = value.mText.toString().toLowerCase();

                    // First match against the whole value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = valueText.split(" |\\n|,|-");
                        final int wordCount = words.length;

                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
        	mItemDatas = (ArrayList<ItemData>)results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
