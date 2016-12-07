package com.yunos.alicontacts.dialpad.smartsearch;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class SearchResult {
    public static final SearchResult EMPTY_RESULT = new SearchResult("", 0);

    public final String mSearchText;
    private ArrayList<Bucket> mResultBuckets;
    private ArrayList<Integer> mSubTotal;

    private int mTotalCount = 0;

    private SearchResult(String searchText, int initListCapacity) {
        mSearchText = searchText;
        mResultBuckets = new ArrayList<Bucket>(initListCapacity);
        mSubTotal = new ArrayList<Integer>(initListCapacity);
    }

    public int getResultCount() {
        return mTotalCount;
    }

    public MatchResult get(int position) {
        int low = 0;
        int high = mResultBuckets.size() - 1;
        int middle = (low + high) >> 1;
        int middleValue;
        while (low < high) {
            middleValue = mSubTotal.get(middle);
            if (position < middleValue) {
                high = middle;
            } else {
                low = middle + 1;
            }
            middle = (low + high) >> 1;
        }
        int bucketIndex = middle == 0 ? position : (position - mSubTotal.get(middle - 1));
        return mResultBuckets.get(middle).getResult(bucketIndex);
    }

    private void add(Bucket bucket) {
        int count = bucket.getResultsCount();
        mResultBuckets.add(bucket);
        mTotalCount += count;
        mSubTotal.add(mTotalCount);
    }

    /**
     * A Bucket stores MatchResult objects that have the same matchWeight.
     * Using buckets, we can reduce the number of objects to sort on first display.
     * We only need to sort the displaying bucket, and let other buckets to be sorted later.
     */
    private static class Bucket {
        public static final Bucket SEPARATOR_PHONE_CONTACTS = makeSeparatorBucket(MatchResult.SEPARATOR_PHONE_CONTACTS);
        public static final Bucket SEPARATOR_CALL_LOGS = makeSeparatorBucket(MatchResult.SEPARATOR_CALL_LOGS);
        public static final Bucket SEPARATOR_YELLOW_PAGE = makeSeparatorBucket(MatchResult.SEPARATOR_YELLOW_PAGE);
        public static final Bucket SEPARATOR_QUICK_CALL = makeSeparatorBucket(MatchResult.SEPARATOR_QUICK_CALL);

        private MatchResult[] mFinalResults = null;
        private ArrayList<MatchResult> mResultsList = new ArrayList<MatchResult>();

        private static Bucket makeSeparatorBucket(MatchResult sep) {
            Bucket bucket = new Bucket();
            bucket.mResultsList.add(sep);
            return bucket;
        }

        public int getResultsCount() {
            return mResultsList.size();
        }

        public MatchResult getResult(int index) {
            if (mFinalResults == null) {
                MatchResult[] tmpResults = mResultsList.toArray(new MatchResult[mResultsList.size()]);
                Arrays.sort(tmpResults, new Comparator<MatchResult>() {
                    @Override
                    public int compare(MatchResult r1, MatchResult r2) {
                        // assume the results in the same bucket have the same matchWeight.
                        // so only compare the name or number.
                        if ((r1.matchPart & MatchResult.MATCH_PART_NAME) == MatchResult.MATCH_PART_NAME) {
                            // the MatchResult.matchWeight contains matchPart info,
                            // so if matchWeight are the same, then matchPart must be same,
                            // and if r1.matchPart has MATCH_PART_NAME bit,
                            // then r2.matchPart must have MATCH_PART_NAME bit.
                            // More: to make Chinese names be displayed before English names,
                            // we use r2.xxx.compareTo(r1.xxx).
                            int result = r2.matchedNamePart.compareToIgnoreCase(r1.matchedNamePart);
                            if (result != 0) {
                                return result;
                            }
                            // If the match parts are the same, then full match name gets first.
                            if (r1.nameFullMatch && r2.nameFullMatch) {
                                // full through to number compare.
                            } else if (r1.nameFullMatch) {
                                return -1;
                            } else if (r2.nameFullMatch) {
                                return 1;
                            } else {
                                result = r2.name.compareToIgnoreCase(r1.name);
                                if (result != 0) {
                                    return result;
                                }
                            }
                        }
                        if (r1.numberFullMatch) {
                            if (r2.numberFullMatch) {
                                return 0;
                            }
                            return -1;
                        }
                        if (r2.numberFullMatch) {
                            return 1;
                        }
                        if (r2.matchedNumberPart != null) {
                            if (r1.matchedNumberPart == null) {
                                return 1;
                            }
                        } else if (r1.matchedNumberPart != null) {
                            return -1;
                        }
                        return r2.phoneNumber.compareToIgnoreCase(r1.phoneNumber);
                    }
                });
                mFinalResults = tmpResults;
            }
            return mFinalResults[index];
        }

    }

    /**
     * A SearchResultBuilder is responsible for adding MatchResult objects, managing buckets and generating SearchResult.
     */
    public static class SearchResultBuilder {
        private static final String TAG = "SearchResultBuilder";
        public final String mSearchText;
        private HashMap<String, MatchResult> mExistingMatches = new HashMap<String, MatchResult>(1024);
        private SortedMap<Integer, Bucket> mMatchedPhoneContacts = new TreeMap<Integer, Bucket>();
        private SortedMap<Integer, Bucket> mMatchedYellowPageContacts = new TreeMap<Integer, Bucket>();
        private Bucket mMatchedCallLog = new Bucket();
        private Bucket mQuickCall = new Bucket();

        public SearchResultBuilder(String searchText) {
            mSearchText = searchText;
        }

        public boolean isKeyExists(String key) {
            return mExistingMatches.containsKey(key);
        }

        public int existKeyCount() {
            return mExistingMatches.size();
        }

        public void addMatchResult(MatchResult mr) {
            mExistingMatches.put(mr.key, mr);
            switch (mr.type) {
            case MatchResult.TYPE_CONTACTS:
                putMatchResultToMap(mMatchedPhoneContacts, mr);
                break;
            case MatchResult.TYPE_YELLOWPAGE:
                putMatchResultToMap(mMatchedYellowPageContacts, mr);
                break;
            case MatchResult.TYPE_CALLOG:
                mMatchedCallLog.mResultsList.add(mr);
                break;
            case MatchResult.TYPE_QUICK_CALL:
                mQuickCall.mResultsList.add(mr);
                break;
            default:
                Log.e(TAG, "addMatchResult: invalid match type: "+mr.type+". ignore.", new Exception());
                break;
            }
        }

        public void addAllMatchResult(Collection<MatchResult> mrs) {
            Iterator<MatchResult> it = mrs.iterator();
            MatchResult mr;
            while (it.hasNext()) {
                mr = it.next();
                if (!isKeyExists(mr.key)) {
                    addMatchResult(mr);
                }
            }
        }

        public int getMatchResultCount() {
            return mExistingMatches.size();
        }

        public SearchResult buildResult() {
            // we do not need the map any more.
            mExistingMatches.clear();
            int phoneContactBucketsCount = mMatchedPhoneContacts.size();
            int callLogBucketsCount = mMatchedCallLog.getResultsCount() > 0 ? 1 : 0;
            int yellowPageBucketsCount = mMatchedYellowPageContacts.size();
            int quickCallBucketsCount = mQuickCall.getResultsCount();
            // We will have up to 4 separators that are not in above counts,
            // so add extra 4 in initial capacity of SearchResult.
            SearchResult sr = new SearchResult(mSearchText, phoneContactBucketsCount + callLogBucketsCount + yellowPageBucketsCount + 4);

            if(quickCallBucketsCount > 0){
                sr.add(Bucket.SEPARATOR_QUICK_CALL);
                sr.add(mQuickCall);
            }

            if (phoneContactBucketsCount > 0) {
                sr.add(Bucket.SEPARATOR_PHONE_CONTACTS);
                putBucketsMapToResult(sr, mMatchedPhoneContacts);
            }

            if (callLogBucketsCount > 0) {
                sr.add(Bucket.SEPARATOR_CALL_LOGS);
                sr.add(mMatchedCallLog);
            }

            if (yellowPageBucketsCount > 0) {
                sr.add(Bucket.SEPARATOR_YELLOW_PAGE);
                putBucketsMapToResult(sr, mMatchedYellowPageContacts);
            }

            return sr;
        }

        private void putBucketsMapToResult(SearchResult sr, SortedMap<Integer, Bucket> bucketsMap) {
            Iterator<Integer> it = bucketsMap.keySet().iterator();
            Bucket bucket;
            while (it.hasNext()) {
                bucket = bucketsMap.get(it.next());
                sr.add(bucket);
            }
        }

        private void putMatchResultToMap(SortedMap<Integer, Bucket> map, MatchResult mr) {
            Integer weightKey = mr.matchWeight;
            Bucket bucket = map.get(weightKey);
            if (bucket == null) {
                bucket = new Bucket();
                map.put(weightKey, bucket);
            }
            bucket.mResultsList.add(mr);
        }

    }

}
