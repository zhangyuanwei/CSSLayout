package me.zhangyuanwei.csslayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.facebook.csslayout.CSSAlign;
import com.facebook.csslayout.CSSConstants;
import com.facebook.csslayout.CSSDirection;
import com.facebook.csslayout.CSSFlexDirection;
import com.facebook.csslayout.CSSJustify;
import com.facebook.csslayout.CSSLayoutContext;
import com.facebook.csslayout.CSSNode.MeasureFunction;
import com.facebook.csslayout.CSSPositionType;
import com.facebook.csslayout.CSSWrap;
import com.facebook.csslayout.MeasureOutput;
import com.facebook.csslayout.Spacing;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by zhangyuanwei on 15/8/22.
 */
public class CSSLayout extends ViewGroup {

    /**
     * CSSNode 计算上下文
     */
    private CSSLayoutContext mCSSLayoutContext;

    /**
     * 当前节点的 CSSNode
     */
    protected CSSNode mCSSNode = null;

    /**
     * 子节点的属性集合
     */
    private ArrayList<ChildProperty> mChildProperties;

    /**
     * 是否为根节点，默认为根节点，被添加到 CSSLayout 后，置为false
     */
    private boolean isRootNode = true;

    /**
     * 当 requestLayout 的时候，是否需要 dirty 所有子节点
     */
    private boolean needDirtyOnRequestLayout = true;

    public CSSLayout(Context context) {
        this(context, null);
    }

    public CSSLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CSSLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCSSNode = getNode();
        mChildProperties = new ArrayList<ChildProperty>(4);
        parseCssAttribute(context, attrs);
        setOnHierarchyChangeListener(onHierarchyChangeListener);
    }

    /**
     * 当 requestLayout 时，因为不知道具体是哪个子节点触发的，所以
     * 我们需要把非 CSSLayout 的子节点对应的 CSSNode 都设置为 dirty
     * CSSLayout 类型的子节点，自己会被 dirty
     */
    @Override
    public void requestLayout() {

        if (needDirtyOnRequestLayout) {
            int count = mChildProperties.size();
            int index;
            ChildProperty prop;
            for (index = 0; index < count; index++) {
                prop = mChildProperties.get(index);
                if (!prop.isCssLayout) {
                    prop.cssNode.dirty();
                }
            }
        }

        requestLayoutWhithoutDirty();
    }

    /**
     * 请求重新布局，不 dirty 节点
     */
    protected void requestLayoutWhithoutDirty() {
        ViewParent parent = getParent();
        if (parent instanceof CSSLayout) {
            CSSLayout parentLayout = ((CSSLayout) parent);
            boolean oldValue = parentLayout.needDirtyOnRequestLayout;
            parentLayout.needDirtyOnRequestLayout = false;
            super.requestLayout();
            parentLayout.needDirtyOnRequestLayout = oldValue;
        } else {
            super.requestLayout();
        }
    }

    /**
     * 计算函数
     * 注意，如果当前节点是 CSSLayout 的子节点，该函数并不会被调用
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // CSSLayout 中，只有根节点需要调用计算函数
        if (isRootNode) {
            // 根据传递的宽高信息设置 CSSNode
            mCSSNode.setMeasureSpec(widthMeasureSpec, heightMeasureSpec);

            // 根据 LayoutParams 填充子节点
            fillChildNode();

            // 执行计算
            if (mCSSLayoutContext == null) {
                mCSSLayoutContext = new CSSLayoutContext();
            }
            mCSSNode.calculateLayout(mCSSLayoutContext);
        }

        // 调用子节点的 measure
        // 因为安卓需要调用measure后, layout 才会生效
        measureChildrenIfNeed();

        // 传递计算结果给上级节点
        float width = mCSSNode.getLayoutWidth();
        float height = mCSSNode.getLayoutHeight();
        setMeasuredDimension((int) width, (int) height);

        if (isRootNode) {
            //将布局标记为已使用
            markLayoutSeen();
        }
    }

    /**
     * 设置子 CssNode 相关属性
     */
    protected void fillChildNode() {
        int count = mChildProperties.size();
        int index;

        ChildProperty prop;
        View child;
        CSSNode node;
        LayoutParams params;

        for (index = 0; index < count; index++) {
            prop = mChildProperties.get(index);
            node = prop.cssNode;
            child = prop.view;

            params = ((LayoutParams) child.getLayoutParams());
            params.fillCSSNode(node);
            node.isMeasured = false;

            // 如果子节点是 CSSLayout，则递归调用
            if (prop.isCssLayout) {
                ((CSSLayout) prop.view).fillChildNode();
            }
        }
    }

    /**
     * 调用子节点的 measure 函数，确保安卓的运行机制正常
     */
    protected void measureChildrenIfNeed() {
        int count = mChildProperties.size();
        int index;

        ChildProperty prop;
        View child;
        CSSNode node;

        int w, h;

        for (index = 0; index < count; index++) {
            prop = mChildProperties.get(index);
            node = prop.cssNode;
            child = prop.view;

            if (!node.isMeasured) {
                w = MeasureSpec.makeMeasureSpec((int) (node.getLayoutWidth() + 0.5), MeasureSpec.EXACTLY);
                h = MeasureSpec.makeMeasureSpec((int) (node.getLayoutHeight() + 0.5), MeasureSpec.EXACTLY);
                child.measure(w, h);
                node.isMeasured = true;
            }
        }
    }

    /**
     * 根据 CSSNode 的计算结果，布局子节点
     *
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = mChildProperties.size();
        int index;

        ChildProperty prop;
        CSSNode node;
        View child;

        float x, y, w, h;

        for (index = 0; index < count; index++) {
            prop = mChildProperties.get(index);
            node = prop.cssNode;
            child = prop.view;

            x = node.getLayoutX();
            y = node.getLayoutY();
            w = node.getLayoutWidth();
            h = node.getLayoutHeight();
            /*
            if (child instanceof CSSLayout) {
                CSSLayout childLayout = ((CSSLayout) child);
                LayoutTransition transition = childLayout.getLayoutTransition();
                childLayout.suppressLayout();
                Assert.assertNull(transition);
                this.layout();
            }
            */
            child.layout((int) (x + 0.5), (int) (y + 0.5), (int) (x + w + 0.5), (int) (y + h + 0.5));
        }
    }


    /**
     * 设置 CSSNode 的 Layout 状态为已使用
     */
    protected void markLayoutSeen() {
        int count = mChildProperties.size();
        int index;
        ChildProperty prop;

        for (index = 0; index < count; index++) {
            prop = mChildProperties.get(index);

            // 如果子节点是 CSSLayout，则递归调用
            // 否则调用对应节点 markLayoutSeen
            if (prop.isCssLayout) {
                ((CSSLayout) prop.view).markLayoutSeen();
            } else if (prop.cssNode.hasNewLayout()) {
                prop.cssNode.markLayoutSeen();
            }
        }

        // 调用当前节点 markLayoutSeen
        if (mCSSNode.hasNewLayout()) {
            mCSSNode.markLayoutSeen();
        }
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

    private void parseCssAttribute(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CSSLayout);

        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.CSSLayout_direction:
                    setDirection(getEnum(a.getInt(attr, 0), CSSDirection.class));
                    break;
                case R.styleable.CSSLayout_flexDirection:
                    setFlexDirection(getEnum(a.getInt(attr, 0), CSSFlexDirection.class));
                    break;
                case R.styleable.CSSLayout_justifyContent:
                    setJustifyContent(getEnum(a.getInt(attr, 0), CSSJustify.class));
                    break;
                case R.styleable.CSSLayout_alignContent:
                    setAlignContent(getEnum(a.getInt(attr, 0), CSSAlign.class));
                    break;
                case R.styleable.CSSLayout_alignItems:
                    setAlignItems(getEnum(a.getInt(attr, 0), CSSAlign.class));
                    break;
                case R.styleable.CSSLayout_flexWrap:
                    setFlexWrap(getEnum(a.getInt(attr, 0), CSSWrap.class));
                    break;

                case R.styleable.CSSLayout_padding:
                    setPadding(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_paddingLeft:
                    setPaddingLeft(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_paddingRight:
                    setPaddingRight(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_paddingTop:
                    setPaddingTop(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_paddingBottom:
                    setPaddingBottom(a.getDimension(attr, 0));
                    break;


                case R.styleable.CSSLayout_borderWidth:
                    setBorderWidth(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_borderLeftWidth:
                    setBorderLeftWidth(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_borderRightWidth:
                    setBorderRightWidth(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_borderTopWidth:
                    setBorderTopWidth(a.getDimension(attr, 0));
                    break;
                case R.styleable.CSSLayout_borderBottomWidth:
                    setBorderBottomWidth(a.getDimension(attr, 0));
                    break;
            }
        }
        a.recycle();
    }


    public void setDirection(CSSDirection direction) {
        mCSSNode.setDirection(direction);
        requestLayoutWhithoutDirty();
    }

    public void setFlexDirection(CSSFlexDirection flexDirection) {
        mCSSNode.setFlexDirection(flexDirection);
        requestLayoutWhithoutDirty();
    }

    public void setJustifyContent(CSSJustify justifyContent) {
        mCSSNode.setJustifyContent(justifyContent);
        requestLayoutWhithoutDirty();
    }

    public void setAlignContent(CSSAlign alignContent) {
        mCSSNode.setAlignContent(alignContent);
        requestLayoutWhithoutDirty();
    }

    public void setAlignItems(CSSAlign alignItems) {
        mCSSNode.setAlignItems(alignItems);
        requestLayoutWhithoutDirty();
    }

    public void setFlexWrap(CSSWrap flexWrap) {
        mCSSNode.setWrap(flexWrap);
        requestLayoutWhithoutDirty();
    }

    public void setPadding(float padding) {
        mCSSNode.setPadding(Spacing.ALL, padding);
        requestLayoutWhithoutDirty();
    }

    public void setPaddingLeft(float paddingLeft) {
        mCSSNode.setPadding(Spacing.LEFT, paddingLeft);
        requestLayoutWhithoutDirty();
    }

    public void setPaddingRight(float paddingRight) {
        mCSSNode.setPadding(Spacing.RIGHT, paddingRight);
        requestLayoutWhithoutDirty();
    }

    public void setPaddingTop(float paddingTop) {
        mCSSNode.setPadding(Spacing.TOP, paddingTop);
        requestLayoutWhithoutDirty();
    }

    public void setPaddingBottom(float paddingBottom) {
        mCSSNode.setPadding(Spacing.BOTTOM, paddingBottom);
        requestLayoutWhithoutDirty();
    }

    public void setBorderWidth(float borderWidth) {
        mCSSNode.setBorder(Spacing.ALL, borderWidth);
        requestLayoutWhithoutDirty();
    }

    public void setBorderLeftWidth(float borderLeftWidth) {
        mCSSNode.setBorder(Spacing.LEFT, borderLeftWidth);
        requestLayoutWhithoutDirty();
    }

    public void setBorderRightWidth(float borderRightWidth) {
        mCSSNode.setBorder(Spacing.RIGHT, borderRightWidth);
        requestLayoutWhithoutDirty();
    }

    public void setBorderTopWidth(float borderTopWidth) {
        mCSSNode.setBorder(Spacing.TOP, borderTopWidth);
        requestLayoutWhithoutDirty();
    }

    public void setBorderBottomWidth(float borderBottomWidth) {
        mCSSNode.setBorder(Spacing.BOTTOM, borderBottomWidth);
        requestLayoutWhithoutDirty();
    }

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
            CSSLayout childLayout;

            int count = layout.getChildCount();
            int index;
            boolean isCssLayout;
            LayoutParams params;
            CSSNode node;

            for (index = 0; index < count; index++) {
                child = layout.getChildAt(index);
                params = ((LayoutParams) child.getLayoutParams());

                //如果 layoutIndex 未定义，则应为新添加的节点
                if (params.layoutIndex == LayoutParams.LAYOUT_INDEX_UNDEFINED) {

                    // 如果子节点为 CSSLayout 则将子节点的 CSSNode 添加到 当前节点的 CSSNode 中
                    // 否则，使用新的节点作为子 CSSNode，并设置计算函数
                    if (child instanceof CSSLayout) {
                        isCssLayout = true;
                        childLayout = ((CSSLayout) child);
                        node = childLayout.mCSSNode;
                        childLayout.isRootNode = false;
                    } else {
                        isCssLayout = false;
                        node = getNode();
                        node.bindingView = child;
                        node.setMeasureFunction(measureFunction);
                    }

                    layout.mCSSNode.addChildAt(node, index);
                    layout.mChildProperties.add(index, ChildProperty.get(child, node, isCssLayout));
                }

                params.layoutIndex = index;
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
            CSSLayout childLayout;
            int count = layout.getChildCount();
            int index;

            ChildProperty prop;
            LayoutParams params;
            CSSNode node;

            params = ((LayoutParams) child.getLayoutParams());
            index = params.layoutIndex;
            params.layoutIndex = LayoutParams.LAYOUT_INDEX_UNDEFINED;

            prop = layout.mChildProperties.remove(index);
            node = layout.mCSSNode.removeChildAt(index);

            if (prop.isCssLayout) {
                childLayout = ((CSSLayout) prop.view);
                childLayout.isRootNode = true;
            } else {
                // 不是 CSSLayout，则 CSSNode 是临时分配的，需要释放
                node.bindingView = null;
                node.setMeasureFunction(null);
                freeNode(node);
            }

            ChildProperty.free(prop);

            // 重新设置索引
            for (/* index = 0 不需要从头开始 */; index < count; index++) {
                child = layout.getChildAt(index);
                params = ((LayoutParams) child.getLayoutParams());
                params.layoutIndex = index;
            }
        }
    };

    /**
     * 非 CSSLayout 子节点的计算函数
     */
    private static final MeasureFunction measureFunction = new MeasureFunction() {

        @Override
        public void measure(com.facebook.csslayout.CSSNode node, float width, boolean isExactly, MeasureOutput measureOutput) {
            CSSNode cssNode = ((CSSNode) node);
            View bindingView = cssNode.bindingView;

            int w, h;
            if (CSSConstants.isUndefined(width)) {
                w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            } else {
                w = MeasureSpec.makeMeasureSpec((int) width, isExactly ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST);
            }

            h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            bindingView.measure(w, h);
            measureOutput.width = bindingView.getMeasuredWidth();
            measureOutput.height = bindingView.getMeasuredHeight();
            cssNode.isMeasured = true;
        }
    };

    public static class CSSNode extends com.facebook.csslayout.CSSNode {
        /* package */ View bindingView = null;
        /* package */ boolean isMeasured = false;

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

        public void setMeasureSpec(int widthMeasureSpec, int heightMeasureSpec) {
            int wMode = MeasureSpec.getMode(widthMeasureSpec);
            int wSize = MeasureSpec.getSize(widthMeasureSpec);

            int hMode = MeasureSpec.getMode(heightMeasureSpec);
            int hSize = MeasureSpec.getSize(heightMeasureSpec);

            switch (wMode) {
                case MeasureSpec.EXACTLY:
                    setStyleWidth(wSize);
                    setMaxWidth(CSSConstants.UNDEFINED);
                    break;
                case MeasureSpec.AT_MOST:
                    setStyleWidth(CSSConstants.UNDEFINED);
                    setMaxWidth(wSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                    setStyleWidth(CSSConstants.UNDEFINED);
                    setMaxWidth(CSSConstants.UNDEFINED);
                    break;
            }

            switch (hMode) {
                case MeasureSpec.EXACTLY:
                    setStyleHeight(hSize);
                    setMaxHeight(CSSConstants.UNDEFINED);
                    break;
                case MeasureSpec.AT_MOST:
                    setStyleHeight(CSSConstants.UNDEFINED);
                    setMaxHeight(hSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                    setStyleHeight(CSSConstants.UNDEFINED);
                    setMaxHeight(CSSConstants.UNDEFINED);
                    break;
            }
        }

        @Override
        public CSSNode getChildAt(int i) {
            return (CSSNode) super.getChildAt(i);
        }

        @Override
        public CSSNode removeChildAt(int i) {
            return (CSSNode) super.removeChildAt(i);
        }

        public void clear() {
            style.direction = CSSDirection.INHERIT;
            style.flexDirection = CSSFlexDirection.COLUMN;
            style.justifyContent = CSSJustify.FLEX_START;
            style.alignContent = CSSAlign.FLEX_START;
            style.alignItems = CSSAlign.STRETCH;
            style.alignSelf = CSSAlign.AUTO;
            style.positionType = CSSPositionType.RELATIVE;
            style.flexWrap = CSSWrap.NOWRAP;
            style.flex = 0;

            style.margin = new Spacing();
            style.padding = new Spacing();
            style.border = new Spacing();

            style.positionTop = CSSConstants.UNDEFINED;
            style.positionBottom = CSSConstants.UNDEFINED;
            style.positionLeft = CSSConstants.UNDEFINED;
            style.positionRight = CSSConstants.UNDEFINED;

            style.width = CSSConstants.UNDEFINED;
            style.height = CSSConstants.UNDEFINED;

            style.minWidth = CSSConstants.UNDEFINED;
            style.minHeight = CSSConstants.UNDEFINED;

            style.maxWidth = CSSConstants.UNDEFINED;
            style.maxHeight = CSSConstants.UNDEFINED;
        }


        @Override
        public void markLayoutSeen() {
            super.markLayoutSeen();
        }


        @Override
        public void dirty() {
            super.dirty();
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        /* package */ static final int LAYOUT_INDEX_UNDEFINED = -1;
        /* package */ int layoutIndex = LAYOUT_INDEX_UNDEFINED;
        private static final int DEFAULT_DIMENSION = WRAP_CONTENT;

        public CSSAlign alignSelf = CSSAlign.AUTO;
        public CSSPositionType position = CSSPositionType.RELATIVE;
        public float flex;

        public float margin = CSSConstants.UNDEFINED;
        public float marginLeft = CSSConstants.UNDEFINED;
        public float marginRight = CSSConstants.UNDEFINED;
        public float marginTop = CSSConstants.UNDEFINED;
        public float marginBottom = CSSConstants.UNDEFINED;


        public float top = CSSConstants.UNDEFINED;
        public float bottom = CSSConstants.UNDEFINED;
        public float left = CSSConstants.UNDEFINED;
        public float right = CSSConstants.UNDEFINED;

        //public float width = CSSConstants.UNDEFINED;
        //public float height = CSSConstants.UNDEFINED;

        public float minWidth = CSSConstants.UNDEFINED;
        public float minHeight = CSSConstants.UNDEFINED;

        public float maxWidth = CSSConstants.UNDEFINED;
        public float maxHeight = CSSConstants.UNDEFINED;

        public LayoutParams() {
            this(DEFAULT_DIMENSION, DEFAULT_DIMENSION);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            parseCssAttribute(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        /* package */ void fillCSSNode(CSSNode node) {
            if (width != WRAP_CONTENT && width != MATCH_PARENT) {
                node.setStyleWidth(width);
            } else {
                node.setStyleWidth(CSSConstants.UNDEFINED);
            }

            if (height != WRAP_CONTENT && height != MATCH_PARENT) {
                node.setStyleHeight(height);
            } else {
                node.setStyleHeight(CSSConstants.UNDEFINED);
            }

            node.setMinWidth(minWidth);
            node.setMinHeight(minHeight);
            node.setMaxWidth(maxWidth);
            node.setMaxHeight(maxHeight);

            node.setPositionLeft(left);
            node.setPositionRight(right);
            node.setPositionTop(top);
            node.setPositionBottom(bottom);

            node.setMargin(Spacing.ALL, margin);
            node.setMargin(Spacing.LEFT, marginLeft);
            node.setMargin(Spacing.RIGHT, marginRight);
            node.setMargin(Spacing.TOP, marginTop);
            node.setMargin(Spacing.BOTTOM, marginBottom);

            node.setAlignSelf(alignSelf);
            node.setFlex(flex);
            node.setPositionType(position);
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            //默认为 MATCH_PARENT
            width = a.getLayoutDimension(widthAttr, DEFAULT_DIMENSION);
            height = a.getLayoutDimension(heightAttr, DEFAULT_DIMENSION);
        }

        private void parseCssAttribute(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.CSSLayout_LayoutParams);

            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                switch (attr) {
                    //case R.styleable.CSSLayout_LayoutParams_layout_width:
                    //    node.setStyleWidth(a.getDimension(attr, 0));
                    //    break;
                    //case R.styleable.CSSLayout_LayoutParams_layout_height:
                    //    node.setStyleHeight(a.getDimension(attr, 0));
                    //    break;

                    case R.styleable.CSSLayout_LayoutParams_layout_minWidth:
                        minHeight = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_minHeight:
                        minHeight = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_maxWidth:
                        maxWidth = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_maxHeight:
                        maxHeight = a.getDimension(attr, 0);
                        break;

                    case R.styleable.CSSLayout_LayoutParams_layout_left:
                        left = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_right:
                        right = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_top:
                        top = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_bottom:
                        bottom = a.getDimension(attr, 0);
                        break;

                    case R.styleable.CSSLayout_LayoutParams_layout_margin:
                        margin = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_marginLeft:
                        marginLeft = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_marginRight:
                        marginRight = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_marginTop:
                        marginTop = a.getDimension(attr, 0);
                        break;
                    case R.styleable.CSSLayout_LayoutParams_layout_marginBottom:
                        marginBottom = a.getDimension(attr, 0);
                        break;

                    case R.styleable.CSSLayout_LayoutParams_layout_alignSelf:
                        alignSelf = getEnum(a.getInt(attr, 0), CSSAlign.class);
                        break;

                    case R.styleable.CSSLayout_LayoutParams_layout_flex:
                        flex = a.getFloat(attr, 0);
                        break;

                    case R.styleable.CSSLayout_LayoutParams_layout_position:
                        position = getEnum(a.getInt(attr, 0), CSSPositionType.class);
                        break;
                }
            }
            a.recycle();
        }
    }

    private static class ChildProperty {

        public View view;
        public CSSNode cssNode;
        public boolean isCssLayout;

        private static LinkedList<ChildProperty> statePool = new LinkedList<ChildProperty>();

        public static ChildProperty get(View childView, CSSNode node, boolean isCssLayout) {
            ChildProperty state;
            if (statePool.size() > 0) {
                state = statePool.removeLast();
            } else {
                state = new ChildProperty();
            }
            state.view = childView;
            state.cssNode = node;
            state.isCssLayout = isCssLayout;
            return state;
        }

        public static void free(ChildProperty state) {
            state.view = null;
            state.cssNode = null;
            state.isCssLayout = false;
            statePool.addLast(state);
        }

    }

    /**
     * CSSNode 池,用于减少内存开销
     */
    private static LinkedList<CSSNode> nodePool = new LinkedList<CSSNode>();

    private static CSSNode getNode() {
        if (nodePool.size() > 0) {
            CSSNode node = nodePool.removeLast();
            node.clear();
            return node;
        }
        return new CSSNode();
    }

    private static void freeNode(CSSNode node) {
        nodePool.addLast(node);
    }

    private static <T extends Enum<T>> T getEnum(int value, Class<T> enumType) {
        T[] constants = enumType.getEnumConstants();
        return constants[value];
    }
}
