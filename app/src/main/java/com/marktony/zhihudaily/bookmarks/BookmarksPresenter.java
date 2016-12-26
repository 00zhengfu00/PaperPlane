package com.marktony.zhihudaily.bookmarks;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.marktony.zhihudaily.adapter.BookmarksAdapter;
import com.marktony.zhihudaily.bean.DoubanMomentNews;
import com.marktony.zhihudaily.bean.GuokrHandpickNews;
import com.marktony.zhihudaily.bean.ZhihuDailyNews;
import com.marktony.zhihudaily.db.DatabaseHelper;
import com.marktony.zhihudaily.detail.DoubanDetailActivity;
import com.marktony.zhihudaily.detail.GuokrDetailActivity;
import com.marktony.zhihudaily.detail.ZhihuDetailActivity;
import com.marktony.zhihudaily.homepage.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.marktony.zhihudaily.adapter.BookmarksAdapter.TYPE_DOUBAN_NORMAL;
import static com.marktony.zhihudaily.adapter.BookmarksAdapter.TYPE_DOUBAN_WITH_HEADER;
import static com.marktony.zhihudaily.adapter.BookmarksAdapter.TYPE_GUOKR_NORMAL;
import static com.marktony.zhihudaily.adapter.BookmarksAdapter.TYPE_GUOKR_WITH_HEADER;
import static com.marktony.zhihudaily.adapter.BookmarksAdapter.TYPE_ZHIHU_NORMAL;
import static com.marktony.zhihudaily.adapter.BookmarksAdapter.TYPE_ZHIHU_WITH_HEADER;

/**
 * Created by lizhaotailang on 2016/12/23.
 */

public class BookmarksPresenter implements BookmarksContract.Presenter {

    private BookmarksContract.View view;
    private Context context;
    private Gson gson;

    private ArrayList<DoubanMomentNews.posts> doubanList;
    private ArrayList<GuokrHandpickNews.result> guokrList;
    private ArrayList<ZhihuDailyNews.Question> zhihuList;

    private ArrayList<Integer> types;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public BookmarksPresenter(Context context, BookmarksContract.View view) {
        this.context = context;
        this.view = view;
        this.view.setPresenter(this);
        gson = new Gson();
        dbHelper = new DatabaseHelper(context, "History.db", null, 5);
        db = dbHelper.getWritableDatabase();

        zhihuList = new ArrayList<>();
        guokrList = new ArrayList<>();
        doubanList = new ArrayList<>();

        types = new ArrayList<>();

    }

    @Override
    public void start() {

    }

    @Override
    public void loadResults(boolean refresh) {

        if (!refresh) {
            view.showLoading();
        } else {
            zhihuList.clear();
            guokrList.clear();
            doubanList.clear();
            types.clear();
        }

        checkForFreshData();

        view.showResults(zhihuList, guokrList, doubanList, types);

        view.stopLoading();

    }

    @Override
    public void startZhihuReading(int position) {
        context.startActivity(new Intent(context, ZhihuDetailActivity.class)
                .putExtra("id",zhihuList.get(position - 1).getId())
        );
    }

    @Override
    public void startGuokrReading(int position) {
        GuokrHandpickNews.result item = guokrList.get(position - zhihuList.size() - 2);
        context.startActivity(new Intent(context, GuokrDetailActivity.class)
                .putExtra("id", item.getId())
                .putExtra("headlineImageUrl", item.getHeadline_img())
                .putExtra("title", item.getTitle())
        );

    }

    @Override
    public void startDoubanReading(int position) {
        DoubanMomentNews.posts item = doubanList.get(position - zhihuList.size() - guokrList.size() - 3);
        Intent intent = new Intent(context, DoubanDetailActivity.class);
        intent.putExtra("id", item.getId());
        intent.putExtra("title", item.getTitle());
        if (item.getThumbs().size() == 0){
            intent.putExtra("image", "");
        } else {
            intent.putExtra("image", item.getThumbs().get(0).getMedium().getUrl());
        }

        context.startActivity(intent);

    }

    @Override
    public void checkForFreshData() {

        // every first one of the 3 lists is with header
        // add them in advance

        types.add(TYPE_ZHIHU_WITH_HEADER);
        Cursor cursor = db.rawQuery("select * from Zhihu where bookmark = ?", new String[]{"1"});
        if (cursor.moveToFirst()) {
            do {
                ZhihuDailyNews.Question question = gson.fromJson(cursor.getString(cursor.getColumnIndex("zhihu_news")), ZhihuDailyNews.Question.class);
                zhihuList.add(question);
                types.add(TYPE_ZHIHU_NORMAL);
            } while (cursor.moveToNext());
        }

        types.add(TYPE_GUOKR_WITH_HEADER);
        cursor = db.rawQuery("select * from Guokr where bookmark = ?", new String[]{"1"});
        if (cursor.moveToFirst()) {
            do {
                GuokrHandpickNews.result result = gson.fromJson(cursor.getString(cursor.getColumnIndex("guokr_news")), GuokrHandpickNews.result.class);
                guokrList.add(result);
                types.add(TYPE_GUOKR_NORMAL);
            } while (cursor.moveToNext());
        }

        types.add(TYPE_DOUBAN_WITH_HEADER);
        cursor = db.rawQuery("select * from Douban where bookmark = ?", new String[]{"1"});
        if (cursor.moveToFirst()) {
            do {
                DoubanMomentNews.posts post = gson.fromJson(cursor.getString(cursor.getColumnIndex("douban_news")), DoubanMomentNews.posts.class);
                doubanList.add(post);
                types.add(TYPE_DOUBAN_NORMAL);
            } while (cursor.moveToNext());
        }

        cursor.close();

    }

    @Override
    public void feelLucky() {
        Random random = new Random();
        int p = random.nextInt(types.size());
        while (true) {
            if (types.get(p) == BookmarksAdapter.TYPE_ZHIHU_NORMAL) {
                startZhihuReading(p);
                break;
            } else if (types.get(p) == BookmarksAdapter.TYPE_GUOKR_NORMAL) {
                startGuokrReading(p);
                break;
            } else if (types.get(p) == BookmarksAdapter.TYPE_DOUBAN_NORMAL) {
                startDoubanReading(p);
                break;
            } else {
                p = random.nextInt(types.size());
            }
        }
    }

}