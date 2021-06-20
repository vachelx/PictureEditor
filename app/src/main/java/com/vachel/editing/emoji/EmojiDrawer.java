package com.vachel.editing.emoji;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.astuetz.PagerSlidingTabStrip;
import com.vachel.editing.R;


public class EmojiDrawer extends LinearLayout {
    private FrameLayout[] gridLayouts = new FrameLayout[Emoji.PAGES.length];
    private Emoji emoji;
    private ViewPager pager;
    private PagerSlidingTabStrip strip;
    private IEmojiCallback mCallback;

    @SuppressWarnings("unused")
    public EmojiDrawer(Context context) {
        super(context);
        initialize();
    }

    @SuppressWarnings("unused")
    public EmojiDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @SuppressWarnings("unused")
    @TargetApi(VERSION_CODES.HONEYCOMB)
    public EmojiDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public EmojiDrawer bindCallback(IEmojiCallback callback) {
        mCallback = callback;
        return this;
    }

    private void initialize() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.emoji_drawer, this, true);
        initializeResources();
        initializeEmojiGrid();
    }

    private void initializeResources() {
        this.pager = findViewById(R.id.emoji_pager);
        this.strip = findViewById(R.id.tabs);
        ImageButton backspace = findViewById(R.id.backspace);
        this.emoji = Emoji.getInstance(getContext().getApplicationContext());
        backspace.setOnClickListener(new BackspaceClickListener());
    }

    private void initializeEmojiGrid() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < gridLayouts.length; i++) {
            gridLayouts[i] = (FrameLayout) inflater.inflate(R.layout.emoji_grid_layout, pager, false);
            final GridView gridView = gridLayouts[i].findViewById(R.id.emoji);
            gridLayouts[i].setTag(gridView);
            gridView.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.emoji_drawer_size) + 2 * getResources().getDimensionPixelSize(R.dimen.emoji_drawer_item_padding));
            gridView.setAdapter(new EmojiGridAdapter(i));
            gridView.setOnItemClickListener(new EmojiClickListener());
        }

        pager.setAdapter(new EmojiPagerAdapter());
        strip.setTabPaddingLeftRight(getResources().getDimensionPixelSize(R.dimen.emoji_drawer_left_right_padding));
        strip.setAllCaps(false);
        strip.setShouldExpand(true);
        strip.setUnderlineColorResource(R.color.emoji_tab_underline);
        strip.setIndicatorColorResource(R.color.emoji_tab_indicator);
        strip.setIndicatorHeight(getResources().getDimensionPixelSize(R.dimen.emoji_drawer_indicator_height));

        strip.setViewPager(pager);
    }

    private class EmojiClickListener implements AdapterView.OnItemClickListener {

        public EmojiClickListener() {
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Integer unicodePoint = (Integer) view.getTag();
            if (mCallback != null) {
                final char[] chars = Character.toChars(unicodePoint);
                String characters = new String(chars);
                mCallback.onEmojiClick(characters);
            }
        }
    }

    private class BackspaceClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (mCallback != null) {
                mCallback.onBackClick();
                return;
            }
        }

    }

    private class EmojiGridAdapter extends BaseAdapter {
        private final int page;
        private final int emojiSize;

        public EmojiGridAdapter(int page) {
            this.page = page;
            emojiSize = (int) getResources().getDimension(R.dimen.emoji_drawer_size);
        }

        @Override
        public int getCount() {
            return Emoji.PAGES[page].length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final ImageView view;
            final int pad = getResources().getDimensionPixelSize(R.dimen.emoji_drawer_item_padding);
            if (convertView != null && convertView instanceof ImageView) {
                view = (ImageView) convertView;
            } else {
                ImageView imageView = new ImageView(getContext());
                imageView.setLayoutParams(new AbsListView.LayoutParams(emojiSize + 2 * pad, emojiSize + 2 * pad));
                view = imageView;
            }

            final Drawable drawable;
            final Integer unicodeTag;
            unicodeTag = Emoji.PAGES[page][position];
            drawable = emoji.getEmojiDrawable(new Emoji.DrawInfo(page, position), new Emoji.InvalidatingPageLoadedListener(view));

            view.setImageDrawable(drawable);
            view.setPadding(pad, pad, pad, pad);
            view.setTag(unicodeTag);
            view.setScaleType(ImageView.ScaleType.FIT_XY);
            view.setMaxHeight(emojiSize + 2 * pad);
            view.setMaxWidth(emojiSize + 2 * pad);
            view.setScaleType(ImageView.ScaleType.FIT_XY);
            return view;
        }
    }

    private class EmojiPagerAdapter extends PagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        @Override
        public int getCount() {
            return gridLayouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        public Object instantiateItem(ViewGroup container, int position) {
            if (position < 0 || position >= gridLayouts.length)
                throw new AssertionError("position out of range!");
            container.addView(gridLayouts[position], 0);
            return gridLayouts[position];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(gridLayouts[position]);
        }

        @Override
        public int getPageIconResId(int i) {
            switch (i) {
                case 0:
                    return R.drawable.emoji_category_smile;
                case 1:
                    return R.drawable.emoji_category_flower;
                case 2:
                    return R.drawable.emoji_category_bell;
                case 3:
                    return R.drawable.emoji_category_car;
                case 4:
                    return R.drawable.emoji_category_symbol;
                default:
                    return 0;
            }
        }
    }
}
