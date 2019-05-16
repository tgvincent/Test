package me.yokeyword.itemtouchhelperdemo.demochannel;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import me.yokeyword.itemtouchhelperdemo.R;
import me.yokeyword.itemtouchhelperdemo.helper.OnDragVHListener;
import me.yokeyword.itemtouchhelperdemo.helper.OnItemMoveListener;

/**
 * 拖拽排序 + 增删
 * Created by Panda on 19/4/28.
 * 移动动画的坐标要考虑自身margin和父Viewpadding因素, 已完善
 * 长按拖拽松开, 造成Item背景变化不消失BUG
 */
public class ChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnItemMoveListener {
    /**
     * 我的频道 标题部分
     */
    public static final int TYPE_MY_CHANNEL_HEADER = 0;
    /**
     * 我的频道
     */
    public static final int TYPE_MY = 1;
    /**
     * 其他频道 标题部分
     */
    public static final int TYPE_OTHER_CHANNEL_HEADER = 2;
    /**
     * 其他频道
     */
    public static final int TYPE_OTHER = 3;
    private static final String TAG = "ChannelAdapter";
    /**
     * 我的频道之前的header数量  该demo中 即标题部分 为 1
     */
    private static final int COUNT_PRE_MY_HEADER = 1;
    /**
     * 其他频道之前的header数量  该demo中 即标题部分 为 COUNT_PRE_MY_HEADER + 1
     */
    private static final int COUNT_PRE_OTHER_HEADER = COUNT_PRE_MY_HEADER + 1;

    /**
     * 动画时间
     */
    private static final long ANIM_TIME = 360L;
    /**
     * touch 间隔时间  用于分辨是否是 "点击"
     */
    private static final long SPACE_TIME = 100;
    /**
     * touch 点击开始时间
     */
    private long startTime;
    private LayoutInflater mInflater;
    private ItemTouchHelper mItemTouchHelper;

    /**
     * 是否为 编辑 模式
     */
    private boolean isEditMode;

    /**
     * "我的频道"数据源
     */
    private List<ChannelEntity> mMyChannelItems;
    /**
     * "其他频道"数据源
     */
    private List<ChannelEntity> mOtherChannelItems;

    // 我的频道点击事件
    private OnMyChannelItemClickListener mChannelItemClickListener;

    private Context mContext;
    private Handler delayHandler = new Handler();

    public ChannelAdapter(Context context, ItemTouchHelper helper, List<ChannelEntity> mMyChannelItems, List<ChannelEntity> mOtherChannelItems) {
        mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mItemTouchHelper = helper;
        this.mMyChannelItems = mMyChannelItems;
        this.mOtherChannelItems = mOtherChannelItems;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {    // 我的频道 标题部分
            return TYPE_MY_CHANNEL_HEADER;
        } else if (position == mMyChannelItems.size() + 1) {    // 其他频道 标题部分
            return TYPE_OTHER_CHANNEL_HEADER;
        } else if (position > 0 && position < mMyChannelItems.size() + 1) {
            return TYPE_MY;
        } else {
            return TYPE_OTHER;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view;
        switch (viewType) {
            case TYPE_MY_CHANNEL_HEADER:
                view = mInflater.inflate(R.layout.item_my_channel_header, parent, false);
                final MyChannelHeaderViewHolder holder = new MyChannelHeaderViewHolder(view);
                holder.tvBtnEdit.setOnClickListener(new View.OnClickListener() {//切换编辑状态
                    @Override
                    public void onClick(View v) {
                        if (!isEditMode) {//不可编辑
                            startEditMode((RecyclerView) parent);
                            holder.tvBtnEdit.setText(R.string.finish);
                        } else {
                            cancelEditMode((RecyclerView) parent);
                            holder.tvBtnEdit.setText(R.string.edit);
                        }
                    }
                });
                return holder;

            case TYPE_MY:
                view = mInflater.inflate(R.layout.item_my, parent, false);
                final MyViewHolder myHolder = new MyViewHolder(view);

                myHolder.myRelate.setOnClickListener(new View.OnClickListener() {//我的频道Item点击事件
                    @Override
                    public void onClick(final View v) {
                        int position = myHolder.getAdapterPosition();
                        if (isEditMode) {
                            if (position > 5) {
                                RecyclerView recyclerView = ((RecyclerView) parent);
                                //其他频道第一个Item
                                View targetView = recyclerView.getLayoutManager().findViewByPosition(mMyChannelItems.size() + COUNT_PRE_OTHER_HEADER);
                                //我的频道要移动的Item
                                View currentView = recyclerView.getLayoutManager().findViewByPosition(position);
                                // 如果targetView不在屏幕内,则indexOfChild为-1  此时不需要添加动画,因为此时notifyItemMoved自带一个向目标移动的动画
                                // 如果在屏幕内,则添加一个位移动画
                                if (recyclerView.indexOfChild(targetView) >= 0) {
                                    int targetX, targetY;

                                    RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
                                    int spanCount = ((GridLayoutManager) manager).getSpanCount();//列数
                                    // 移动后 高度将变化 (我的频道Grid 最后一个item在新的一行第一个)
                                    if ((mMyChannelItems.size() - COUNT_PRE_MY_HEADER) % spanCount == 0) {
                                        Log.i(TAG, "最后一个item在新的一行第一个");
                                        /**
                                         * 验证正确(x, y)
                                         */
                                        View preTargetView = recyclerView.getLayoutManager().findViewByPosition(mMyChannelItems.size() + COUNT_PRE_OTHER_HEADER - 1);
                                        targetX = preTargetView.getLeft() + recyclerView.getPaddingLeft();

                                        GridLayoutManager.LayoutParams currentViewLayoutParams = (GridLayoutManager.LayoutParams) currentView.getLayoutParams();
                                        GridLayoutManager.LayoutParams targetViewLayoutParams = (GridLayoutManager.LayoutParams) targetView.getLayoutParams();
                                        int offset = currentView.getHeight() + currentViewLayoutParams.bottomMargin + currentViewLayoutParams.topMargin;
                                        targetY = preTargetView.getTop() - offset + preTargetView.getHeight() + targetViewLayoutParams.topMargin;
                                    } else {
                                        Log.i(TAG, "最后一个item 不在新的一行第一个");
                                        targetX = targetView.getLeft();
                                        targetY = targetView.getTop();
                                    }

                                    moveMyToOther(myHolder);
                                    startAnimation(recyclerView, currentView, targetX, targetY);

                                } else {
                                    moveMyToOther(myHolder);
                                }
                            } else {
                                Toast.makeText(mContext, mMyChannelItems.get(position - COUNT_PRE_MY_HEADER).getName(), Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            mChannelItemClickListener.onItemClick(v, position - COUNT_PRE_MY_HEADER);
                        }
                    }
                });

                myHolder.myRelate.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(final View v) {
                        int position = myHolder.getAdapterPosition();
                        if (!isEditMode) {
                            if (position > 5) {
                                RecyclerView recyclerView = ((RecyclerView) parent);
                                startEditMode(recyclerView);

                                // header 按钮文字 改成 "完成"
                                View view = recyclerView.getChildAt(0);
                                if (view == recyclerView.getLayoutManager().findViewByPosition(0)) {
                                    TextView tvBtnEdit = (TextView) view.findViewById(R.id.tv_btn_edit);
                                    tvBtnEdit.setText(R.string.finish);
                                }
                            } else {
                                Toast.makeText(mContext, mMyChannelItems.get(position).getName(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        if (position > 5) {
                            mItemTouchHelper.startDrag(myHolder);//开始拖拽
                        } else {
                            Toast.makeText(mContext, mMyChannelItems.get(position).getName(), Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });

                myHolder.myRelate.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (isEditMode) {
                            switch (MotionEventCompat.getActionMasked(event)) {
                                case MotionEvent.ACTION_DOWN:
                                    startTime = System.currentTimeMillis();
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    if (System.currentTimeMillis() - startTime > SPACE_TIME) {
                                        mItemTouchHelper.startDrag(myHolder);
                                    }
                                    break;
                                case MotionEvent.ACTION_CANCEL:
                                case MotionEvent.ACTION_UP:
                                    startTime = 0;
                                    break;
                            }

                        }
                        return false;
                    }
                });
                return myHolder;

            case TYPE_OTHER_CHANNEL_HEADER:
                view = mInflater.inflate(R.layout.item_other_channel_header, parent, false);
                return new RecyclerView.ViewHolder(view) {
                };

            case TYPE_OTHER:
                view = mInflater.inflate(R.layout.item_other, parent, false);

                final OtherViewHolder otherHolder = new OtherViewHolder(view);
                otherHolder.itemRelate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView recyclerView = ((RecyclerView) parent);
                        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
                        int currentPiosition = otherHolder.getAdapterPosition();
                        // 如果RecyclerView滑动到底部,移动的目标位置的y轴 - height
                        View currentView = manager.findViewByPosition(currentPiosition);
                        // 目标位置的前一个item  即当前MyChannel的最后一个
                        View preTargetView = manager.findViewByPosition(mMyChannelItems.size() - 1 + COUNT_PRE_MY_HEADER);

                        // 如果targetView不在屏幕内,则为-1  此时不需要添加动画,因为此时notifyItemMoved自带一个向目标移动的动画
                        // 如果在屏幕内,则添加一个位移动画
                        if (recyclerView.indexOfChild(preTargetView) >= 0) {
                            GridLayoutManager.LayoutParams preTargetViewLp = (GridLayoutManager.LayoutParams) preTargetView.getLayoutParams();
                            int targetX = preTargetView.getLeft();
                            int targetY = preTargetView.getTop();
                            Log.i(TAG, "targetX: " + targetX + " targetY: " + targetY);

                            int targetPosition = mMyChannelItems.size() - 1 + COUNT_PRE_OTHER_HEADER;

                            GridLayoutManager gridLayoutManager = ((GridLayoutManager) manager);
                            int spanCount = gridLayoutManager.getSpanCount();
                            // target 在最后一行第一个
                            if ((targetPosition - COUNT_PRE_MY_HEADER) % spanCount == 0) {
                                View targetView = manager.findViewByPosition(targetPosition);
                                GridLayoutManager.LayoutParams targetViewLp = (GridLayoutManager.LayoutParams) targetView.getLayoutParams();
                                //实践正确(x, y)
                                targetX = targetView.getLeft() + targetViewLp.leftMargin + recyclerView.getPaddingLeft();
                                targetY = targetView.getTop() + targetViewLp.topMargin + preTargetViewLp.bottomMargin;
                                Log.i(TAG, "target 在最后一行第一个 targetX: " + targetX + " targetY: " + targetY);
                            } else {
                                /**
                                 * 实践正确(x) 要加上 前一个View的leftMargin 和 自身的leftMargin
                                 * (因为item一样, 所以省事用的同一个布局属性, 如果item不一样, 要用 targetView的布局属性)
                                 */
                                targetX += preTargetView.getWidth() + preTargetViewLp.leftMargin + preTargetViewLp.rightMargin;
                                Log.i(TAG, "target 不是最后一行第一个 targetX: " + targetX + " targetY: " + targetY);
                                // 最后一个item可见
                                if (gridLayoutManager.findLastVisibleItemPosition() == getItemCount() - 1) {
                                    Log.i(TAG, "最后一个item可见");
                                    // 最后的item在最后一行第一个位置
                                    if ((getItemCount() - 1 - mMyChannelItems.size() - COUNT_PRE_OTHER_HEADER) % spanCount == 0) {
                                        Log.i(TAG, "最后的item在最后一行第一个位置");
                                        // RecyclerView实际高度 > 屏幕高度 && RecyclerView实际高度 < 屏幕高度 + item.height
                                        int firstVisiblePostion = gridLayoutManager.findFirstVisibleItemPosition();
                                        if (firstVisiblePostion == 0) {
                                            Log.i(TAG, "第一个Item, 即我的频道标题可见");
                                            // FirstCompletelyVisibleItemPosition == 0 即 内容不满一屏幕 , targetY值不需要变化
                                            // // FirstCompletelyVisibleItemPosition != 0 即 内容满一屏幕 并且 可滑动 , targetY值 + firstItem.getTop
                                            if (gridLayoutManager.findFirstCompletelyVisibleItemPosition() != 0) {
                                                Log.i(TAG, "内容满一屏幕 并且 可滑动");
                                                /**
                                                 * 布局recyclerView.getChildAt(0)(即"我的频道"标题)如果它的布局最外层有margin属性, 要计算一下
                                                 * 因为布局中没有, 所以不用计算
                                                 */
                                                int offset = (-recyclerView.getChildAt(0).getTop()) - recyclerView.getPaddingTop();
                                                /**
                                                 * 实践正确(y) 要加上 目标View上方一个View的bottomMargin 和 自身的topMargin
                                                 * (因为item一样, 所以省事用的同一个布局属性, 如果item不一样,
                                                 * 要用 targetView上方View的布局属性)
                                                 */
                                                targetY += offset + preTargetViewLp.topMargin + preTargetViewLp.bottomMargin;
                                            }
                                        } else { // 在这种情况下 并且 RecyclerView高度变化时(即可见第一个item的 position != 0),
                                            // 移动后, targetY值  + 一个item的高度
                                            Log.i(TAG, "第一个Item, 即我的频道标题不可见");
                                            //实践正确(y)
                                            targetY += preTargetView.getHeight() + preTargetViewLp.topMargin + preTargetViewLp.bottomMargin;
                                        }
                                    }
                                } else {
                                    System.out.println("current--No");
                                }
                            }

                            // 如果当前位置是otherChannel可见的最后一个
                            // 并且 当前位置不在grid的第一个位置
                            // 并且 目标位置不在grid的第一个位置

                            // 则 需要延迟250秒 notifyItemMove , 这是因为这种情况 , 并不触发ItemAnimator , 会直接刷新界面
                            // 导致我们的位移动画刚开始,就已经notify完毕,引起不同步问题
                            if (currentPiosition == gridLayoutManager.findLastVisibleItemPosition()
                                    && (currentPiosition - mMyChannelItems.size() - COUNT_PRE_OTHER_HEADER) % spanCount != 0
                                    && (targetPosition - COUNT_PRE_MY_HEADER) % spanCount != 0) {
                                moveOtherToMyWithDelay(otherHolder);
                            } else {
                                moveOtherToMy(otherHolder);
                            }
                            startAnimation(recyclerView, currentView, targetX, targetY);

                        } else {
                            moveOtherToMy(otherHolder);//todo 当数据很多时有一个bug, 难复现
                        }
                    }
                });
                return otherHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MyViewHolder) {
            MyViewHolder myHolder = (MyViewHolder) holder;
            AppCompatTextView tv = (AppCompatTextView) ((MyViewHolder) holder).textView;
//            @SuppressLint("RestrictedApi") int maxTextSize = tv.getAutoSizeMaxTextSize();//这两个数不会变化
//            @SuppressLint("RestrictedApi") int minTextSize = tv.getAutoSizeMinTextSize();
            float tvSize = tv.getTextSize();
            float temp = tv.getTextSize();
            Log.i(TAG, "My前 " + position + " " + tv.getText() + " =>  tvSize: " + tvSize);
            tv.setText(mMyChannelItems.get(position - COUNT_PRE_MY_HEADER).getName());
            if (isEditMode) {
                myHolder.imgEdit.setVisibility(View.VISIBLE);
            } else {
                myHolder.imgEdit.setVisibility(View.INVISIBLE);
            }
            if (position < 6) {
                tv.setTextColor(mContext.getResources().getColor(R.color.tip));
            } else {
                tv.setTextColor(mContext.getResources().getColor(R.color.black));

            }
            tvSize = tv.getTextSize();
            Log.i(TAG, "My后 " + position + " " + tv.getText() + " =>  tvSize: " + tvSize);

            //变为原来的字体大小重置
            tv.setTextSize(temp);
            tvSize = tv.getTextSize();
            Log.i(TAG, "My再后 " + position + " " + tv.getText() + " =>  tvSize: " + tvSize);
        } else if (holder instanceof OtherViewHolder) {
            //position-我的频道数量-其他频道之前的header数量
            AppCompatTextView tv = (AppCompatTextView) ((OtherViewHolder) holder).textView;
            TextView add = ((OtherViewHolder) holder).add;

            Log.i(TAG, "Other addTextView textSize = " + add.getTextSize());
//            @SuppressLint("RestrictedApi") int maxTextSize = tv.getAutoSizeMaxTextSize();//这两个数不会变化
//            @SuppressLint("RestrictedApi") int minTextSize = tv.getAutoSizeMinTextSize();
            float tvSize = tv.getTextSize();
            float temp = tv.getTextSize();
            Log.i(TAG, "Other前 " + position + " " + tv.getText() + " => tvSize: " + tvSize);
            //建议: 如果这个"+"字体和其他内容字体不一样, 可以参考SpannableString
            tv.setText("+ " + mOtherChannelItems.get(position - mMyChannelItems.size() - COUNT_PRE_OTHER_HEADER).getName());
            tvSize = tv.getTextSize();
            Log.i(TAG, "Other后 " + position + " " + tv.getText() + " => tvSize: " + tvSize);


            //变为原来的字体大小
            tv.setTextSize(temp);//没用...(具体可以看这个方法注释)wrap_content
            /**
             * 为什么没用?(因为原来布局用的就是wrap_content)
             * 因为当TextView是自适应wrap_content的时候会根据文字字体大小和文字长度确定TextView宽度(但不超过父View宽度).
             * 这是TextView刚Inflate刚加载进来的时候, 而涉及到RecycleView的Item复用问题, 这里就比较复杂了:
             * 因为布局中设置自适应wrap_content, 这会导致: 最初create构建Item的时候, TextView的文字内容长度不一样, 会导致
             * TextView的mMeasuredWidth(真实可用宽度--呈现文字的区域)不一样, 并且这个Item在以后的复用时,
             * 这个mMeasuredWidth都不会进行改变(除非写代码进行重新测量绘制布局).
             * 有一个场景, 例如A这个Item的文字宽度很短, 只有2个字, 那么create出来的时候, mMeasuredWidth的宽度是2个字的宽度(例如为50像素,每个字25像素).
             * 当滑动界面时, 这个A Item被回收复用, 前面我们提到, mMeasuredWidth真实可用宽度在构建之后不会进行改变(除非用代码进行重测绘制布局),
             * 那么如果这个Item复用时用来呈现一个5个文字长度的内容会怎样? 在设置了自动最大字体和最小字体属性情况下, 这5个文字只能每个字占50/5=10像素)
             * 所以文字变小了, 以此类推...
             *
             * 这里使用了比较简单的处理方法: 布局TextView使用 match_parent 配置, 争取create出来的时候, 跟父布局一样宽
             * 同时只是用一个TextView呈现 加号+和内容, 可以设置同一个TextView设置不同字体大小, 颜色, 超链接....可以参考:
             * https://www.jianshu.com/p/bdea3f4c661a
             * https://www.cnblogs.com/bdsdkrb/p/5715438.html
             */
            tvSize = tv.getTextSize();
            Log.i(TAG, "Other再后 " + position + " " + tv.getText() + " => tvSize: " + tvSize);
        } else if (holder instanceof MyChannelHeaderViewHolder) {

            MyChannelHeaderViewHolder headerHolder = (MyChannelHeaderViewHolder) holder;
            if (isEditMode) {
                headerHolder.tvBtnEdit.setText(R.string.finish);
            } else {
                headerHolder.tvBtnEdit.setText(R.string.edit);
            }
        }
    }

    @Override
    public int getItemCount() {
        // 我的频道  标题 + 我的频道.size + 其他频道 标题 + 其他频道.size
        return mMyChannelItems.size() + mOtherChannelItems.size() + COUNT_PRE_OTHER_HEADER;
    }

    /**
     * 开始增删动画
     */
    private void startAnimation(RecyclerView recyclerView, final View currentView, float targetX, float targetY) {
        final ViewGroup viewGroup = (ViewGroup) recyclerView.getParent();
        final ImageView mirrorView = addMirrorView(viewGroup, recyclerView, currentView);

        Animation animation = getTranslateAnimator(
                targetX - currentView.getLeft(), targetY - currentView.getTop());
        currentView.setVisibility(View.INVISIBLE);
        mirrorView.startAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewGroup.removeView(mirrorView);
                if (currentView.getVisibility() == View.INVISIBLE) {
                    currentView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * 我的频道 移动到 其他频道
     *
     * @param myHolder
     */
    private void moveMyToOther(MyViewHolder myHolder) {
        int position = myHolder.getAdapterPosition();

        int startPosition = position - COUNT_PRE_MY_HEADER;
        if (startPosition > mMyChannelItems.size() - 1) {
            return;
        }
        ChannelEntity item = mMyChannelItems.get(startPosition);
        mMyChannelItems.remove(startPosition);
        mOtherChannelItems.add(0, item);

        notifyItemMoved(position, mMyChannelItems.size() + COUNT_PRE_OTHER_HEADER);
    }

    /**
     * 其他频道 移动到 我的频道
     *
     * @param otherHolder
     */
    private void moveOtherToMy(OtherViewHolder otherHolder) {
        int position = processItemRemoveAdd(otherHolder);
        if (position == -1) {
            return;
        }
        notifyItemMoved(position, mMyChannelItems.size() - 1 + COUNT_PRE_MY_HEADER);
    }

    /**
     * 其他频道 移动到 我的频道 伴随延迟
     *
     * @param otherHolder
     */
    private void moveOtherToMyWithDelay(OtherViewHolder otherHolder) {
        final int position = processItemRemoveAdd(otherHolder);
        if (position == -1) {
            return;
        }
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyItemMoved(position, mMyChannelItems.size() - 1 + COUNT_PRE_MY_HEADER);
            }
        }, ANIM_TIME);
    }

    private int processItemRemoveAdd(OtherViewHolder otherHolder) {
        int position = otherHolder.getAdapterPosition();

        int startPosition = position - mMyChannelItems.size() - COUNT_PRE_OTHER_HEADER;
        if (startPosition > mOtherChannelItems.size() - 1) {
            return -1;
        }
        ChannelEntity item = mOtherChannelItems.get(startPosition);
        mOtherChannelItems.remove(startPosition);
        mMyChannelItems.add(item);
        return position;
    }


    /**
     * 添加需要移动的 镜像View
     */
    private ImageView addMirrorView(ViewGroup parent, RecyclerView recyclerView, View view) {
        /**
         * 我们要获取cache首先要通过setDrawingCacheEnable方法开启cache，然后再调用getDrawingCache方法就可以获得view的cache图片了。
         buildDrawingCache方法可以不用调用，因为调用getDrawingCache方法时，若果cache没有建立，系统会自动调用buildDrawingCache方法生成cache。
         若想更新cache, 必须要调用destoryDrawingCache方法把旧的cache销毁，才能建立新的。
         当调用setDrawingCacheEnabled方法设置为false, 系统也会自动把原来的cache销毁。
         */
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(true);
        final ImageView mirrorView = new ImageView(recyclerView.getContext());
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        mirrorView.setImageBitmap(bitmap);
        view.setDrawingCacheEnabled(false);
        int[] locations = new int[2];
        view.getLocationOnScreen(locations);
        int[] parenLocations = new int[2];
        recyclerView.getLocationOnScreen(parenLocations);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight());
        params.setMargins(locations[0], locations[1] - parenLocations[1], 0, 0);
        parent.addView(mirrorView, params);

        return mirrorView;
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        ChannelEntity item = mMyChannelItems.get(fromPosition - COUNT_PRE_MY_HEADER);
        mMyChannelItems.remove(fromPosition - COUNT_PRE_MY_HEADER);
        mMyChannelItems.add(toPosition - COUNT_PRE_MY_HEADER, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * 开启编辑模式
     *
     * @param parent
     */
    private void startEditMode(RecyclerView parent) {
        isEditMode = true;
        int visibleChildCount = parent.getChildCount();//方法仅返回其所包含的直接控件的数量
        for (int i = 6; i < visibleChildCount; i++) {
            View view = parent.getChildAt(i);//getChildAt()这个方法,只能get到屏幕显示的部分.
            ImageView imgEdit = (ImageView) view.findViewById(R.id.img_edit);
            if (imgEdit != null) {
                imgEdit.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 完成编辑模式
     *
     * @param parent
     */
    private void cancelEditMode(RecyclerView parent) {
        isEditMode = false;

        int visibleChildCount = parent.getChildCount();
        for (int i = 0; i < visibleChildCount; i++) {
            View view = parent.getChildAt(i);
            ImageView imgEdit = (ImageView) view.findViewById(R.id.img_edit);
            if (imgEdit != null) {
                imgEdit.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * 获取位移动画
     */
    private TranslateAnimation getTranslateAnimator(float targetX, float targetY) {
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.ABSOLUTE, targetX,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.ABSOLUTE, targetY);
        // RecyclerView默认移动动画250ms 这里设置360ms 是为了防止在位移动画结束后 remove(view)过早 导致闪烁
        translateAnimation.setDuration(ANIM_TIME);
        translateAnimation.setFillAfter(true);
        return translateAnimation;
    }

    public void setOnMyChannelItemClickListener(OnMyChannelItemClickListener listener) {
        this.mChannelItemClickListener = listener;
    }

    interface OnMyChannelItemClickListener {
        void onItemClick(View v, int position);
    }

    /**
     * 我的频道
     */
    class MyViewHolder extends RecyclerView.ViewHolder implements OnDragVHListener {
        private TextView textView;
        private ImageView imgEdit;
        private RelativeLayout myRelate;

        public MyViewHolder(View itemView) {
            super(itemView);
            textView = (AppCompatTextView) itemView.findViewById(R.id.tv);
            imgEdit = (ImageView) itemView.findViewById(R.id.img_edit);
            myRelate = (RelativeLayout) itemView.findViewById(R.id.my_relate);
        }

        /**
         * item 被选中时
         */
        @Override
        public void onItemSelected() {
            textView.setBackgroundResource(R.drawable.bg_channel_p);
        }

        /**
         * item 取消选中时
         */
        @Override
        public void onItemFinish() {
            textView.setBackgroundResource(R.drawable.bg_channel);
        }
    }

    /**
     * 其他频道
     */
    class OtherViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private TextView add;
        private RelativeLayout itemRelate;

        public OtherViewHolder(View itemView) {
            super(itemView);
            textView = (AppCompatTextView) itemView.findViewById(R.id.tv);
            add = itemView.findViewById(R.id.add);
            itemRelate = (RelativeLayout) itemView.findViewById(R.id.item_relate);
        }
    }

    /**
     * 我的频道  标题部分
     */
    class MyChannelHeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvBtnEdit;

        public MyChannelHeaderViewHolder(View itemView) {
            super(itemView);
            tvBtnEdit = (TextView) itemView.findViewById(R.id.tv_btn_edit);
        }
    }
}
