package com.mojtaba_shafaei.android.lovSimple;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class LovSimple extends AppCompatDialogFragment{

private final String TAG = "LovSimple";

public interface Item{

  String getCode();

  String getDes();

  int getPriority();

  void setPriority(int priority);

}

static Typeface TYPEFACE_IRANSANS_BOLD, TYPEFACE_IRANSANS_NORMAL;
//<editor-fold desc="ButterKnife">
private AppCompatEditText searchView;
private AppCompatImageButton btnClearText;
private RecyclerView recyclerView;
private ContentLoadingProgressBar progressBar;
private AppCompatTextView tvMessage;
private AppCompatImageButton btnBack;
//</editor-fold>

private LovSimpleAdapter adapter;
private final CompositeDisposable mDisposable = new CompositeDisposable();
/////////////////////////////////////
private CharSequence searchViewHint;
private CharSequence mSearchText;
private OnResultListener mOnResultListener;
private Dialog.OnCancelListener mOnCancelListener;
private Dialog.OnDismissListener mOnDismissListener;

private final PublishSubject<Lce<List<Item>>> mItemsSubject = PublishSubject.create();
private final BehaviorSubject<String> mQuerySubject = BehaviorSubject.create();

/////////////////////////////////////
//
public static LovSimple create(CharSequence searchViewHint, CharSequence searchText){
  LovSimple lovSimple = new LovSimple();
  lovSimple.searchViewHint = searchViewHint;
  lovSimple.mSearchText = StringUtils.defaultIfBlank(searchText);
  return lovSimple;
}

@Override
public int show(FragmentTransaction transaction, String tag){
  return super.show(transaction, tag);
}

@Override
public void onCreate(@Nullable Bundle savedInstanceState){
  super.onCreate(savedInstanceState);
  setStyle(DialogFragment.STYLE_NO_TITLE, R.style.ThemeOverlay_AppCompat_Light);
}

@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup __,
    @Nullable Bundle savedInstanceState){

  View view = inflater.inflate(R.layout.lov_simple_activity_all_lov, null);
  TYPEFACE_IRANSANS_BOLD = Typeface.createFromAsset(getResources().getAssets(), "IRANSansMobile_Bold.ttf");
  TYPEFACE_IRANSANS_NORMAL = Typeface.createFromAsset(getResources().getAssets(), "IRANSansMobile.ttf");

  initUi(view);

  searchView.setHint(StringUtils.defaultIfBlank(searchViewHint));
  searchView.setText(StringUtils.defaultIfBlank(mSearchText));

  LinearLayoutManager linearLayoutManager = new LinearLayoutManager(inflater.getContext());
  recyclerView.setLayoutManager(linearLayoutManager);
  recyclerView.setHasFixedSize(true);
  adapter = new LovSimpleAdapter(recyclerView,
                                 (position, data) -> {
                                   try{
                                     onResult(data);
                                     dismissAllowingStateLoss();
                                   } catch(Exception e){
                                     Log.e(TAG, "onListItemClicked", e);
                                   }

                                 }, LayoutInflater.from(inflater.getContext()));

  adapter.setHasStableIds(true);

  tvMessage.setTypeface(TYPEFACE_IRANSANS_NORMAL);
  tvMessage.setText(getString(R.string.lov_simple_no_data1p));
  recyclerView.setAdapter(adapter);

  btnBack.setOnClickListener(new OnClickListener(){
    @Override
    public void onClick(View v){
      onCancel(LovSimple.this.getDialog());
      hideSoftKeyboard(searchView);

      btnBack.getViewTreeObserver()
          .addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout(){
              if(btnBack.getViewTreeObserver().isAlive()){
                btnBack.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                float value =
                    ((View) btnBack.getParent()).getRight() - btnBack.getRight() + btnBack
                        .getWidth();
                ObjectAnimator animator = ObjectAnimator.ofFloat(btnBack, "translationX", value);
                animator.setInterpolator(new DecelerateInterpolator(.8F));
                animator.setDuration(300);
                animator.addListener(new AnimatorListenerAdapter(){
                  @Override
                  public void onAnimationEnd(Animator animation){
                    super.onAnimationEnd(animation);
                    dismissAllowingStateLoss();
                  }
                });
                animator.start();
              }
            }
          });
    }
  });

  searchView.setText(StringUtils.defaultIfBlank(mSearchText));
  btnClearText.setOnClickListener(v -> searchView.setText(""));
  return view;
}

@Override
public void onActivityCreated(@Nullable Bundle savedInstanceState){
  super.onActivityCreated(savedInstanceState);
  btnBack.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.lov_simple_ic_arrow_back_grey_600_24dp));
  btnClearText.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.lov_simple_ic_close_grey_600_24dp));
}

private void onResult(Item data){
  if(mOnResultListener != null){
    mOnResultListener.onResult(data);
  }
}

private void hideSoftKeyboard(AppCompatEditText searchView){
  if(searchView != null){
    InputMethodManager inputManager = (InputMethodManager)
        searchView.getContext().getSystemService(INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromInputMethod(searchView.getWindowToken(), 0);
    inputManager.hideSoftInputFromWindow(searchView.getApplicationWindowToken(), 0);

    getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

    searchView.clearFocus();
    searchView.setSelected(false);
  }
}

private Observable<String> getQueryIntent(){
  return RxTextView.afterTextChangeEvents(searchView)
      .observeOn(AndroidSchedulers.mainThread())
      .map(this::setButtonClearTextVisibilityAndReturnQuery)
      .observeOn(Schedulers.computation())
      .throttleWithTimeout(600, TimeUnit.MILLISECONDS)
      .map(query -> StringUtils.replaceAll(query, "\\s(\\s)+", StringUtils.SPACE))//remove duplicate spaces
      .map(String::trim)
      .distinctUntilChanged()
      .map(it -> {
        mQuerySubject.onNext(it);
        return it;
      });
}

@Override
public void onStart(){
  super.onStart();
  tvMessage.setVisibility(View.GONE);
  try{
    mDisposable.add(
        Observable.combineLatest(getQueryIntent(), mItemsSubject, InputDataKeeper::new)
            .switchMap(it -> {
              if(it.data == null){
                return Observable.just(Lce.<QueryDataKeeper>error(new Error(getString(R.string.lov_simple_no_data1p))));
              }

              if(it.data.isLoading()){
                return Observable.just(Lce.<QueryDataKeeper>loading());
              } else if(it.data.hasError()){
                return Observable.just(Lce.<QueryDataKeeper>error(it.data.getError()));
              }

              //filter results base on query
              if(it.query.length() > 1){
                final String[] queries = StringUtils.split(it.query, StringUtils.SPACE);

                List<Item> results = new ArrayList<>();
                int priority;

                for(Item item : it.data.getData()){
                  priority = 0;

                  if(StringUtils.startsWithIgnoreCase(item.getDes(), it.query)){
                    priority++;
                  }

                  if(StringUtils.startsWithIgnoreCase(item.getDes(), it.query + ' ')){
                    priority++;
                  }

                  if(StringUtils.equalsIgnoreCase(item.getDes(), it.query)){
                    priority++;
                  }

                  if(StringUtils.endsWithIgnoreCase(item.getDes(), it.query)){
                    priority++;
                  }

                  if(StringUtils.endsWithIgnoreCase(item.getDes(), ' ' + it.query)){
                    priority++;
                  }

                  if(StringUtils.containsIgnoreCase(item.getDes(), it.query)){
                    priority++;
                  }

                  for(String k : queries){
                    priority += (StringUtils.countMatches(item.getDes().toUpperCase(), StringUtils.wrap(k.toUpperCase(), StringUtils.SPACE)));
                  }

                  for(String k : queries){
                    priority += (StringUtils.countMatches(item.getDes().toUpperCase(), k.toUpperCase()));
                  }

                  item.setPriority(priority);
                  //Add item if it is desired one.
                  if(priority > 0){
                    results.add(item);
                  }
                }
                return Observable.just(Lce.data(new QueryDataKeeper(queries, results)));
              } else{
                return Observable.just(Lce.data(new QueryDataKeeper(null, it.data.getData())));
              }
            })
            .observeOn(AndroidSchedulers.mainThread())
//            .startWith(Lce.loading())
            .subscribeWith(new DisposableObserver<Lce<QueryDataKeeper>>(){
              @Override
              public void onNext(Lce<QueryDataKeeper> lce){
                if(isDisposed()){
                  return;
                }
                Log.d(TAG, "onNext: " + lce.toString());

                try{
                  tvMessage.setVisibility(View.GONE);

                  showContentLoading(lce.isLoading());
                  if(lce.hasError()){
                    showError(lce.getError().getMessage());
                  } else{
                    hideErrors();
                  }

                  final QueryDataKeeper queryDataKeeper = lce.getData();
                  if(queryDataKeeper != null){
                    adapter.setHighlightFor(queryDataKeeper.queries);
                    adapter.setData(queryDataKeeper.data);

                    if(CollectionUtils.isEmpty(queryDataKeeper.data)){
                      tvMessage.setText(getString(R.string.lov_simple_no_data1p));
                      tvMessage.setVisibility(VISIBLE);
                    } else{
                      tvMessage.setVisibility(View.GONE);
                    }
                  } else{
                    adapter.clearData();
                  }
                } catch(Exception e){
                  Log.e(TAG, "onNext: ", e);
                }
              }

              @Override
              public void onError(Throwable e){
                Log.e(TAG, "onError() called with: e = [" + e + "],isDisposed() = [" + isDisposed() + "]");
                if(isDisposed()){
                  return;
                }
                showContentLoading(false);
                showInternetError();
              }

              @Override
              public void onComplete(){
              }
            })
    );
  } catch(Exception e){
    Log.e(TAG, "onCreate:RxSearch ", e);
  }
}

private String setButtonClearTextVisibilityAndReturnQuery(TextViewAfterTextChangeEvent event){
  btnClearText.setVisibility(StringUtils.isEmpty(event.editable()) ? View.GONE : View.VISIBLE);
  return StringUtils.defaultIfBlank(event.editable());
}

private void showContentLoading(boolean b){
  progressBar.setVisibility(b ? VISIBLE : View.GONE);
}

private void initUi(View root){
  searchView = root.findViewById(R.id.lov_simple_search_view);
  btnClearText = root.findViewById(R.id.lov_simple_btn_clear_search);
  recyclerView = root.findViewById(R.id.lov_simple_list);
  progressBar = root.findViewById(R.id.lov_simple_progressBar);
  tvMessage = root.findViewById(R.id.lov_simple_tv_message);
  root = root.findViewById(R.id.lov_simple_root);
  btnBack = root.findViewById(R.id.lov_simple_btn_back);

  ViewCompat.setLayoutDirection(root, ViewCompat.LAYOUT_DIRECTION_RTL);

  progressBar.setVisibility(View.GONE);
  searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

  btnBack.getViewTreeObserver()
      .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
        @Override
        public void onGlobalLayout(){
          if(btnBack.getViewTreeObserver().isAlive()){
            btnBack.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            ObjectAnimator animator = ObjectAnimator.ofFloat(btnBack, "translationX", 0);
            animator.setInterpolator(new DecelerateInterpolator(1.5F));
            animator.setStartDelay(500);
            animator.setDuration(300);
            animator.start();
          }
        }
      });
}

@UiThread
private void hideErrors(){
  tvMessage.setVisibility(View.GONE);
}

@UiThread
private void showInternetError(){
  tvMessage.setVisibility(VISIBLE);
  tvMessage.setText(getString(R.string.lov_simple_no_internet_connection));
}

@UiThread
private void showError(String error){
  tvMessage.setVisibility(VISIBLE);
  tvMessage.setText(error);
}

public LovSimple setOnResultListener(OnResultListener mOnResultListener){
  this.mOnResultListener = mOnResultListener;
  return this;
}

public LovSimple setOnCancelListener(OnCancelListener mOnCancelListener){
  this.mOnCancelListener = mOnCancelListener;
  return this;
}

public LovSimple setOnDismissListener(OnDismissListener mOnDismissListener){
  this.mOnDismissListener = mOnDismissListener;
  return this;
}

/**
 * Use this method to change LOV's data according to entered query text specially with online API.<br/>
 *
 * Just use this method or {@link #setItems(Lce)}
 *
 * @see #setItems(Lce)
 */
public void setItems(Observable<Lce<List<Item>>> itemsObservable){
  itemsObservable.subscribe(mItemsSubject);
}

/**
 * @param lceItems an instance of {@code Lce<List<Item>>}.<br/>
 *
 *                 Use this method to initialize LOV's data once.<br/>
 *
 *                 Just use this method or {@link #setItems(Observable)}
 * @see #setItems(Observable)
 */
public void setItems(Lce<List<Item>> lceItems){
  mItemsSubject.onNext(lceItems);
}

public Observable<String> getQueries(){
  return mQuerySubject
      .subscribeOn(Schedulers.io());
}

@Override
public void onPause(){
  hideSoftKeyboard(searchView);
  super.onPause();
  dismissAllowingStateLoss();
}

@Override
public void onDestroyView(){
  try{
    mOnResultListener = null;
    recyclerView.setAdapter(null);
    mDisposable.clear();
  } catch(Exception e){
    e.printStackTrace();
  }
  super.onDestroyView();
}

@Override
public void onDismiss(DialogInterface dialog){
  if(mOnDismissListener != null){
    mOnDismissListener.onDismiss(dialog);
  }
  super.onDismiss(dialog);
}

@Override
public void onCancel(DialogInterface dialog){
  if(mOnCancelListener != null){
    mOnCancelListener.onCancel(dialog);
  }
  super.onCancel(dialog);
}

public interface OnResultListener{

  void onResult(Item item);
}

private class InputDataKeeper{

  String query;
  Lce<List<Item>> data;

  InputDataKeeper(String query, Lce<List<Item>> data){
    this.query = query;
    this.data = data;
  }
}

private class QueryDataKeeper{

  String[] queries;
  List<Item> data;

  QueryDataKeeper(String[] queries, List<Item> data){
    this.queries = queries;
    this.data = data;
  }
}

public abstract static class Lce<T>{

  public abstract boolean isLoading();

  public abstract boolean hasError();

  public abstract Throwable getError();

  public abstract T getData();

  @Override
  public String toString(){
    return new ToStringBuilder(this)
        .append("Loading", isLoading())
        .append("hasError", hasError())
        .append("error", getError())
        .append("data", (getData() instanceof List) ? "listSize=" + CollectionUtils.size(getData()) :
            (getData() instanceof QueryDataKeeper) ? CollectionUtils.size(((QueryDataKeeper) getData()).data) : getData())
        .toString();
  }

  public static <T> Lce<T> data(final T data){
    return new Lce<T>(){
      @Override
      public boolean isLoading(){
        return false;
      }

      @Override
      public boolean hasError(){
        return false;
      }

      @Override
      public Throwable getError(){
        return null;
      }

      @Override
      public T getData(){
        return data;
      }
    };
  }

  public static <T> Lce<T> error(final Throwable error){
    return new Lce<T>(){
      @Override
      public boolean isLoading(){
        return false;
      }

      @Override
      public boolean hasError(){
        return true;
      }

      @Override
      public Throwable getError(){
        return error;
      }

      @Override
      public T getData(){
        return null;
      }
    };
  }

  public static <T> Lce<T> loading(){
    return new Lce<T>(){
      @Override
      public boolean isLoading(){
        return true;
      }

      @Override
      public boolean hasError(){
        return false;
      }

      @Override
      public Throwable getError(){
        return null;
      }

      @Override
      public T getData(){
        return null;
      }
    };
  }
}
}