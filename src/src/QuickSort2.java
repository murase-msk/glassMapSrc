package src;

import java.util.ArrayList;

/**
 * クイックソートに関するクラス.
 * @author murase
 *
 */
public class QuickSort2<T extends Number,F extends Number>{
	private T[] _arr1;// ソートする配列.
	private F[] _arr2;//　_arrと連動する変数.
	private ArrayList<T> _arrayList1;// ソートする配列.
	private ArrayList<F> _arrayList2;//　_arrと連動する変数.
	
	/**
	 * コンストラクタ
	 * @param aArr		ソートする配列
	 * @param aArr2		それと連動する配列
	 */
	public QuickSort2 (T[] aArr, F[] aArr2) {
		_arr1 = aArr;
		_arr2 = aArr2;
        quickSort(0, _arr1.length-1);
	}
	
	
	/**
	 * コンストラクタ
	 * @param aArrayList1
	 * @param aArrayList2
	 */
	public QuickSort2 (ArrayList<T> aArrayList1, ArrayList<F> aArrayList2){
		_arrayList1 = new ArrayList<>(aArrayList1);
		_arrayList2 = new ArrayList<>(aArrayList2);
		quickSortArray(0, _arrayList1.size()-1);
	}
	
	/**
	 * コンストラクタ
	 * @param aArrayList1
	 * @param aArrayList2
	 * @param descFlg
	 */
	public QuickSort2 (ArrayList<T> aArrayList1, ArrayList<F> aArrayList2, boolean descFlg){
		_arrayList1 = new ArrayList<>(aArrayList1);
		_arrayList2 = new ArrayList<>(aArrayList2);
		if(descFlg){
			quickSortArrayReverse(0, _arrayList1.size()-1);
		}
		else{
			quickSortArray(0, _arrayList1.size()-1);
		}
	}
	
	/**
	 * 1つの配列だけ
	 * @param aArrayList
	 * @param descFlg
	 */
	public QuickSort2 (ArrayList<T> aArrayList, boolean descFlg){
		_arrayList1 = new ArrayList<>(aArrayList);
		if(descFlg){
			quickSortArrayReverse_(0, _arrayList1.size()-1);
		}
		else{
			quickSortArray_(0, _arrayList1.size()-1);
		}

	}
	
	
    //基本挿入法（クイックソート）*********************************
    private void quickSort(int left, int right){
        if (left <= right) {
        	T p = _arr1[(left+right) / 2];
            int l = left;
            int r = right;
            
            while(l <= r) {
                while(_arr1[l].doubleValue() < p.doubleValue()){ l++; }
                while(_arr1[r].doubleValue() > p.doubleValue()){ r--; }
                
                if (l <= r) {
                    T tmp = _arr1[l];
                    _arr1[l] = _arr1[r];
                    _arr1[r] = tmp;
                    
                    F tmp2 = _arr2[l];
                    _arr2[l] = _arr2[r];
                    _arr2[r] = tmp2;
                    
                    l++; 
                    r--;
                }
            }
    
            quickSort(left, r);
            quickSort(l, right);
        }
    }
    
    
  //基本挿入法（クイックソート）*********************************
    //昇順ソート
    private void quickSortArray(int left, int right){
        if (left <= right) {
            T p = _arrayList1.get((left+right) / 2);
            int l = left;
            int r = right;
            
            while(l <= r) {
                while(_arrayList1.get(l).doubleValue() < p.doubleValue()){ l++; }
                while(_arrayList1.get(r).doubleValue() > p.doubleValue()){ r--; }
                
                if (l <= r) {
                    T tmp = _arrayList1.get(l);
                    _arrayList1.set(l, _arrayList1.get(r));
                    _arrayList1.set(r, tmp);
                    
                    F tmp2 = _arrayList2.get(l);
                    _arrayList2.set(l, _arrayList2.get(r));
                    _arrayList2.set(r, tmp2);
                    
                    l++; 
                    r--;
                }
            }
    
            quickSortArray(left, r);
            quickSortArray(l, right);
        }
    }
    
    //基本挿入法（クイックソート）*********************************
    //降順ソート.
    private void quickSortArrayReverse(int left, int right){
        if (left <= right) {
            T p = _arrayList1.get((left+right) / 2);
            int l = left;
            int r = right;
            
            while(l <= r) {
                while(_arrayList1.get(l).doubleValue() > p.doubleValue()){ l++; }
                while(_arrayList1.get(r).doubleValue() < p.doubleValue()){ r--; }
                
                if (l <= r) {
                    T tmp = _arrayList1.get(l);
                    _arrayList1.set(l, _arrayList1.get(r));
                    _arrayList1.set(r, tmp);
                    
                    F tmp2 = _arrayList2.get(l);
                    _arrayList2.set(l, _arrayList2.get(r));
                    _arrayList2.set(r, tmp2);
                    
                    l++; 
                    r--;
                }
            }
    
            quickSortArrayReverse(left, r);
            quickSortArrayReverse(l, right);
        }
    }


    
    
    //基本挿入法（クイックソート）*********************************
    //昇順ソート
    private void quickSortArray_(int left, int right){
        if (left <= right) {
            T p = _arrayList1.get((left+right) / 2);
            int l = left;
            int r = right;
            
            while(l <= r) {
                while(_arrayList1.get(l).doubleValue() < p.doubleValue()){ l++; }
                while(_arrayList1.get(r).doubleValue() > p.doubleValue()){ r--; }
                
                if (l <= r) {
                    T tmp = _arrayList1.get(l);
                    _arrayList1.set(l, _arrayList1.get(r));
                    _arrayList1.set(r, tmp);
                    l++; 
                    r--;
                }
            }
    
            quickSortArray_(left, r);
            quickSortArray_(l, right);
        }
    }
    
    //基本挿入法（クイックソート）*********************************
    //降順ソート.
    private void quickSortArrayReverse_(int left, int right){
        if (left <= right) {
            T p = _arrayList1.get((left+right) / 2);
            int l = left;
            int r = right;
            
            while(l <= r) {
                while(_arrayList1.get(l).doubleValue() > p.doubleValue()){ l++; }
                while(_arrayList1.get(r).doubleValue() < p.doubleValue()){ r--; }
                
                if (l <= r) {
                    T tmp = _arrayList1.get(l);
                    _arrayList1.set(l, _arrayList1.get(r));
                    _arrayList1.set(r, tmp);
                    l++; 
                    r--;
                }
            }
    
            quickSortArrayReverse_(left, r);
            quickSortArrayReverse_(l, right);
        }
    }
    
    

    //配列の値を出力するメソッド***********************************
    private void arrayPrintln(int[] arr){
        for(int i=0; i<arr.length; i++){
        	System.out.print(arr[i] + " ");
            //System.out.print(arr2[i]+" "+arr[i] + " ");
        }
        System.out.println("");
    }
    
    public T[] getArr1Value(){
    	return _arr1;
    }
    public F[] getArr2Value(){
    	return _arr2;
    }
    public ArrayList<T> getArrayList1(){
    	return _arrayList1;
    }
    public ArrayList<F> getArrayList2(){
    	return _arrayList2;
    }
}