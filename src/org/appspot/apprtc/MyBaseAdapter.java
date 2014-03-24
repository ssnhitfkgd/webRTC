package org.appspot.apprtc;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MyBaseAdapter extends BaseAdapter {
   protected ArrayList<Object> alObjects = new ArrayList<Object>();
   protected  AbsListView absListView;
   protected  Context mContext;
   private LayoutInflater inflater = null;
	@Override
	public int getCount() {
		return alObjects.size();
	}

	@Override
	public Object getItem(int arg0) {
		return   alObjects.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}
	
	public MyBaseAdapter(Context context){
		inflater = LayoutInflater.from(context);
		this.mContext = context;
	}
	/**
	 * 设置集合
	 * @param alObjects
	 * @param boo
	 */
	 //主线程刷新UI操作
	Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			notifyDataSetChanged();
			super.handleMessage(msg);
	}};
	//设置适配器数据
	public void setList(List<Object> alObjects)
	{
		if(null!=alObjects&&alObjects.size()>0){
			this.alObjects.addAll(alObjects);

		notifyDataSetChanged();
		}
	}
	//适配器添加数据
	public void addObject(Object object)
	{
		if(null!=alObjects){
			this.alObjects.add(object);

		myHandler.sendEmptyMessage(1);
//		notifyDataSetChanged();
		}
	}
	//适配器删除数据
	public void removeObject(int index)
	{
		if(null!=alObjects&&alObjects.size()>index){
			this.alObjects.remove(index);
		myHandler.sendEmptyMessage(1);
//		notifyDataSetChanged();
		}
	}
	
	public int getSize(){
		if(alObjects != null)
			return alObjects.size();
		return 0;
	}
	
	public Object getOBJ(int index){
		if(alObjects != null && alObjects.size() >index)
			return (Object)alObjects.get(index);
		return null;
	}
	//适配器数据操作回调，可用于将当前数据进行显示等等操作
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
//		Log.e("getView", "getView");
		ViewHolder viewHolder = null;
		if(convertView == null){
			convertView = inflater.inflate(R.layout.list_item, parent, false);	
			viewHolder = new ViewHolder();
			viewHolder.nameIndexItemValue = (TextView)convertView.findViewById(R.id.name);
			convertView.setTag(viewHolder);
		}
		else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		Entity indexentry = (Entity)alObjects.get(position);
		String movie_num = indexentry.getname();
//		Log.e("adapter", movie_num);
		viewHolder.nameIndexItemValue.setText(movie_num);
		return convertView;
	}
	public ArrayList<Object> getAlObjects() {
		return alObjects;
	}	

	public void setAlObjects(ArrayList<Object> alObjects) {
		this.alObjects = alObjects;
	}
	public AbsListView getAbsListView() {
	return absListView;
	}

	public void setAbsListView(AbsListView absListView) {
		this.absListView = absListView;
	}
	
	static class ViewHolder{
		TextView nameIndexItemValue;
	}
}
