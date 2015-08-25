package me.zhangyuanwei.csslayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.facebook.csslayout.CSSAlign;
import com.facebook.csslayout.CSSConstants;
import com.facebook.csslayout.CSSFlexDirection;
import com.facebook.csslayout.CSSJustify;
import com.facebook.csslayout.CSSLayoutContext;
import com.facebook.csslayout.CSSNode;
import com.facebook.csslayout.CSSNode.MeasureFunction;
import com.facebook.csslayout.CSSPositionType;
import com.facebook.csslayout.CSSWrap;
import com.facebook.csslayout.MeasureOutput;
import com.facebook.csslayout.Spacing;

import junit.framework.Assert;

/**
 * Created by zhangyuanwei on 15/8/22.
 */
public class CSSLayout extends ViewGroup {

    private static final int UNDEFINED_INDEX = -1;
    /**
     * 当当前 ViewGroup 的子节点变化时，同步 CSSNode 的子节点
     */
    private static final OnHierarchyChangeListener onHierarchyChangeListener = new OnHierarchyChangeListener() {
        /**
         * 在添加 View 的时候添加对应的 CSSNode 到 mCSSNode 上。
         *
         * @param parent
         * @param child
         */
        @Override
        public void onChildViewAdded(View parent, View child) {
            CSSLayout layout = ((CSSLayout) parent);

            int count = layout.getChildCount();
            int index;
            LayoutParams params;
            CSSNode node;

            for (index = 0; index < count; index++) {
                child = layout.getChildAt(index);
                params = ((LayoutParams) child.getLayoutParams());
                node = params.cssNode;
                if (node.parentIndex == UNDEFINED_INDEX) {
                    node.setMeasureFunction(measureFunction);
                    layout.mCSSNode.addChildAt(node, index);
                }
                node.parentIndex = index;
                node.bindingView = child;
            }
        }

        /**
         * 删除 View 时，从 mCSSNode 上删除对应节点。
         *
         * @param parent
         * @param child
         */
        @Override
        public void onChildViewRemoved(View parent, View child) {
            CSSLayout layout = ((CSSLayout) parent);
            int count = layout.getChildCount();
            int index;
            LayoutParams params;
            CSSNode node;

            params = ((LayoutParams) child.getLayoutParams());
            node = params.cssNode;
            index = node.parentIndex;
            layout.mCSSNode.removeChildAt(index);
            node.setMeasureFunction(null);
            node.parentIndex = UNDEFINED_INDEX;
            node.bindingView = null;

            for (index = 0; index < count; index++) {
                child = layout.getChildAt(index);
                params = ((LayoutParams) child.getLayoutParams());
                node = params.cssNode;
                node.parentIndex = index;
            }
        }
    };

    /**
     * 计算函数
     */
    private static final MeasureFunction measureFunction = new MeasureFunction() {

        /**
         * 计算子节点宽高
         * @param node
         * @param width
         * @param measureOutput
         */
        @Override
        public void measure(com.facebook.csslayout.CSSNode node, float width, boolean isExactly, MeasureOutput measureOutput) {
            CSSNode cssNode = ((CSSNode) node);
            View bindingView = cssNode.bindingView;
            ViewGroup.LayoutParams params = bindingView.getLayoutParams();

            int w, h;
            if (CSSConstants.isUndefined(width)) {
                w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            } else {
                w = MeasureSpec.makeMeasureSpec((int) width, isExactly ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST);
            }

            h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            bindingView.measure(w, h);
            cssNode.isMeasured = true;
            measureOutput.width = bindingView.getMeasuredWidth();
            measureOutput.height = bindingView.getMeasuredHeight();
        }
    };

    private CSSLayoutContext mCSSLayoutContext = null;
    private CSSNode mCSSNode = null;

    public CSSLayout(Context context) {
        this(context, null);
    }

    public CSSLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CSSLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mCSSNode = new CSSNode();
        mCSSLayoutContext = new CSSLayoutContext();
        parseCSSAttribute(context, mCSSNode, attrs, true);
        setOnHierarchyChangeListener(onHierarchyChangeListener);
    }

    /*
        public CSSLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
    */

    private static <T extends Enum<T>> T getEnum(int value, Class<T> enumType) {
        T[] constants = enumType.getEnumConstants();
        return constants[value];
    }

    private static void parseCSSAttribute(Context context, CSSNode node, AttributeSet attrs, boolean isParent) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CSSLayout);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            if (isParent) {
                switch (attr) {
                    // 父节点属性
                    case R.styleable.CSSLayout_layout_flexDirection:
                        node.setFlexDirection(getEnum(a.getInt(attr, 0), CSSFlexDirection.class));
                        break;
                    case R.styleable.CSSLayout_layout_justifyContent:
                        node.setJustifyContent(getEnum(a.getInt(attr, 0), CSSJustify.class));
                        break;
                    case R.styleable.CSSLayout_layout_alignItems:
                        node.setAlignItems(getEnum(a.getInt(attr, 0), CSSAlign.class));
                        break;
                    case R.styleable.CSSLayout_layout_alignContent:
                        node.setAlignContent(getEnum(a.getInt(attr, 0), CSSAlign.class));
                        break;
                    case R.styleable.CSSLayout_layout_flexWrap:
                        node.setWrap(getEnum(a.getInt(attr, 0), CSSWrap.class));
                        break;


                    case R.styleable.CSSLayout_layout_padding:
                        node.setPadding(Spacing.ALL, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_paddingLeft:
                        node.setPadding(Spacing.LEFT, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_paddingRight:
                        node.setPadding(Spacing.RIGHT, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_paddingTop:
                        node.setPadding(Spacing.TOP, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_paddingBottom:
                        node.setPadding(Spacing.BOTTOM, a.getDimension(attr, 0));
                        break;


                    case R.styleable.CSSLayout_layout_borderWidth:
                        node.setBorder(Spacing.ALL, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_borderLeftWidth:
                        node.setBorder(Spacing.LEFT, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_borderRightWidth:
                        node.setBorder(Spacing.RIGHT, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_borderTopWidth:
                        node.setBorder(Spacing.TOP, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_borderBottomWidth:
                        node.setBorder(Spacing.BOTTOM, a.getDimension(attr, 0));
                        break;
                }
            } else {
                switch (attr) {
                    //case R.styleable.CSSLayout_layout_width:
                    //    node.setStyleWidth(a.getDimension(attr, 0));
                    //    break;
                    //case R.styleable.CSSLayout_layout_height:
                    //    node.setStyleHeight(a.getDimension(attr, 0));
                    //    break;

                    case R.styleable.CSSLayout_layout_minWidth:
                        node.setMinWidth(a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_minHeight:
                        node.setMinHeight(a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_maxWidth:
                        node.setMaxWidth(a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_maxHeight:
                        node.setMaxHeight(a.getDimension(attr, 0));
                        break;

                    case R.styleable.CSSLayout_layout_left:
                        node.setPositionLeft(a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_right:
                        node.setPositionRight(a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_top:
                        node.setPositionTop(a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_bottom:
                        node.setPositionBottom(a.getDimension(attr, 0));
                        break;

                    case R.styleable.CSSLayout_layout_margin:
                        node.setMargin(Spacing.ALL, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_marginLeft:
                        node.setMargin(Spacing.LEFT, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_marginRight:
                        node.setMargin(Spacing.RIGHT, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_marginTop:
                        node.setMargin(Spacing.TOP, a.getDimension(attr, 0));
                        break;
                    case R.styleable.CSSLayout_layout_marginBottom:
                        node.setMargin(Spacing.BOTTOM, a.getDimension(attr, 0));
                        break;


                    case R.styleable.CSSLayout_layout_alignSelf:
                        node.setAlignSelf(getEnum(a.getInt(attr, 0), CSSAlign.class));
                        break;

                    case R.styleable.CSSLayout_layout_flex:
                        node.setFlex(a.getFloat(attr, 0));
                        break;

                    case R.styleable.CSSLayout_layout_position:
                        node.setPositionType(getEnum(a.getInt(attr, 0), CSSPositionType.class));
                        break;
                }
            }
        }
        a.recycle();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        LayoutParams layoutParams;
        if (p instanceof LayoutParams) {
            layoutParams = ((LayoutParams) p);
        } else {
            layoutParams = new LayoutParams(p);
        }
        return layoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    private void setCSSNodeWidthHeight(int widthMeasureSpec, int heightMeasureSpec) {
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);

        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);

        switch (wMode) {
            case MeasureSpec.EXACTLY:
                mCSSNode.setStyleWidth(wSize);
                mCSSNode.setMaxWidth(CSSConstants.UNDEFINED);
                break;
            case MeasureSpec.AT_MOST:
                mCSSNode.setStyleWidth(CSSConstants.UNDEFINED);
                mCSSNode.setMaxWidth(wSize);
                break;
            case MeasureSpec.UNSPECIFIED:
                mCSSNode.setStyleWidth(CSSConstants.UNDEFINED);
                mCSSNode.setMaxWidth(CSSConstants.UNDEFINED);
                break;
        }

        switch (hMode) {
            case MeasureSpec.EXACTLY:
                mCSSNode.setStyleHeight(hSize);
                mCSSNode.setMaxHeight(CSSConstants.UNDEFINED);
                break;
            case MeasureSpec.AT_MOST:
                mCSSNode.setStyleHeight(CSSConstants.UNDEFINED);
                mCSSNode.setMaxHeight(hSize);
                break;
            case MeasureSpec.UNSPECIFIED:
                mCSSNode.setStyleHeight(CSSConstants.UNDEFINED);
                mCSSNode.setMaxHeight(CSSConstants.UNDEFINED);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setCSSNodeWidthHeight(widthMeasureSpec, heightMeasureSpec);

        int cssCount = mCSSNode.getChildCount();
        int index;
        View child;
        CSSNode node;

        //if (!isLayoutRequested()) {
        //    return;
        //}

        // 设置各个 CSSNode 的属性
        for (index = 0; index < cssCount; index++) {
            node = (CSSNode) mCSSNode.getChildAt(index);
            node.isMeasured = false;
            node.dirty();
        }

        //执行计算
        mCSSNode.calculateLayout(mCSSLayoutContext);

        // FIXME 这几句屏蔽了 CSSLayout 的状态检查
        if (mCSSNode.hasNewLayout()) {
            mCSSNode.markLayoutSeen();
        }

        // 检查是否每个子节点都已经计算
        int w, h;
        for (index = 0; index < cssCount; index++) {
            node = (CSSNode) mCSSNode.getChildAt(index);

            if (!node.isMeasured) {
                child = node.bindingView;
                w = MeasureSpec.makeMeasureSpec((int) node.getLayoutWidth(), MeasureSpec.EXACTLY);
                h = MeasureSpec.makeMeasureSpec((int) node.getLayoutHeight(), MeasureSpec.EXACTLY);
                child.measure(w, h);
                node.isMeasured = true;
            }

            // FIXME 这几句屏蔽了 CSSLayout 的状态检查
            if (node.hasNewLayout()) {
                node.markLayoutSeen();
            }
        }

        // 传递计算结果给上级节点
        float width = mCSSNode.getLayoutWidth();
        float height = mCSSNode.getLayoutHeight();
        setMeasuredDimension((int) width, (int) height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int cssCount = mCSSNode.getChildCount();

        //System.out.println(this.getTag() + ":" + mCSSNode.toString());
        CSSNode node;
        View child;

        int index;
        float x, y, w, h;

        Assert.assertEquals("子节点数量和子 CSSNode 数量不等", count, cssCount);

        //布局子节点
        for (index = 0; index < count; index++) {
            node = (CSSNode) mCSSNode.getChildAt(index);
            child = getChildAt(index);

            x = node.getLayoutX();
            y = node.getLayoutY();
            w = node.getLayoutWidth();
            h = node.getLayoutHeight();

            child.layout((int) (x + 0.5), (int) (y + 0.5), (int) (x + w + 0.5), (int) (y + h + 0.5));
        }

    }

    public static class CSSNode extends com.facebook.csslayout.CSSNode {
        /* package */ boolean isMeasured = false;
        /* package */ int parentIndex = UNDEFINED_INDEX;
        /* package */ View bindingView = null;


        public void setMinWidth(float minWidth) {
            if (!valuesEqual(style.minWidth, minWidth)) {
                style.minWidth = minWidth;
                dirty();
            }
        }

        public void setMinHeight(float minHeight) {
            if (!valuesEqual(style.minHeight, minHeight)) {
                style.minHeight = minHeight;
                dirty();
            }
        }

        public void setMaxWidth(float maxWidth) {
            if (!valuesEqual(style.maxWidth, maxWidth)) {
                style.maxWidth = maxWidth;
                dirty();
            }
        }

        public void setMaxHeight(float maxHeight) {
            if (!valuesEqual(style.maxHeight, maxHeight)) {
                style.maxHeight = maxHeight;
                dirty();
            }
        }

        public void setAlignContent(CSSAlign alignContent) {
            if (!valuesEqual(style.alignContent, alignContent)) {
                style.alignContent = alignContent;
                dirty();
            }
        }

        @Override
        public void dirty() {
            super.dirty();
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        /* package */ CSSNode cssNode;
        private static final int DEFAULT_DIMENSION = WRAP_CONTENT;

        public LayoutParams() {
            this(DEFAULT_DIMENSION, DEFAULT_DIMENSION);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            initCSSNode();
            parseCSSAttribute(c, cssNode, attrs, false);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            initCSSNode();
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            initCSSNode();
        }

        private void initCSSNode() {
            cssNode = new CSSNode();
            if (width != MATCH_PARENT && width != WRAP_CONTENT) {
                cssNode.setStyleWidth(width);
            }

            if (height != MATCH_PARENT && height != WRAP_CONTENT) {
                cssNode.setStyleHeight(height);
            }
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            //默认为 MATCH_PARENT
            width = a.getLayoutDimension(widthAttr, DEFAULT_DIMENSION);
            height = a.getLayoutDimension(heightAttr, DEFAULT_DIMENSION);
        }

    }
}
