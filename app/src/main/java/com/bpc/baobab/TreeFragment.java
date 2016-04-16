package com.bpc.baobab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import java.util.ArrayList;

/**
 * Created on 3/22/2016.
 * modified under graphics and re-imported here
 */
public class TreeFragment extends Fragment {

    //*********** constants
    public static final String TREE_DATA = "TREE DATA";

    //*********** constants
    private float Y0 = 30; // depth of row_0
    private float DY = 100; // the vertical spacing between levels
    private float PAD = 40; // 20 left/right margins - modifies WIDTH
    private float MRS = 25; // 75 minimum regular separation
    private float MES = 131; // 125  minimum expandable separation
    private float Y1 = 23; // depth of pendant
    private float Y2 = 32; // depth of pendant
    private float Y3 = 16; // height of line
    private float TXT = 16; // text size
    private float TXY = 25; // y-offset for writing label
    private float BW = 5; // half the box width - for the red box at labelled vertices
    private float LW = 3; //line width
    private float CHFACTOR = 3.2f; // factor for deciding text width

    private static final String LOGGER = "bpc_tree"; //Log.d(LOGGER, "page = " + real_id + ", member = " + member_id);

    //************** interface
    // Container Activity must implement this interface
    public interface treeListener {
        void toPage(String target);  // jump to this page - given pageID
    }

    //************* vars
    private Rows theRows;
    private float LEFT, RIGHT;
    private ArrayList<String> mTreeData;
    private treeListener mCallback;

    public TreeFragment() {} // generic constructor

    @Override
    public void onStart() {
        super.onStart();
        try {
            mCallback = (treeListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement treeListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tree_fragment, container, false);

        Bundle args = getArguments();
        if (args != null) {
            mTreeData = args.getStringArrayList(TREE_DATA);
        }

        //fetch the display width/height to set as globals WIDTH/HEIGHT
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point outSize = new Point();
        display.getSize(outSize);
        //set global dimensions of display
        int WIDTH = outSize.x;
        int HEIGHT = outSize.y;

        Log.d(LOGGER, "got width " + WIDTH);

        dimensions(WIDTH); // reset these in context

        RIGHT = WIDTH - PAD;
        LEFT = PAD;

        createTree(mTreeData);

        LinearLayout v1 = (LinearLayout) v.findViewById(R.id.tree_material);

        TreeView mTV = new TreeView(getActivity(), theRows, WIDTH, HEIGHT);

        mTV.setFocusable(true);
        mTV.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN != event.getAction()) {
                    return false;
                }
    //Log.d(LOGGER, "X = "+event.getX() +", Y = "+event.getY() );
                theRows.goTo(event.getX(),event.getY());
                return true;
            }
        });

        // add this view to v1 ...
        v1.addView(mTV);
        return v;
    }


    /**
     * depending on width, determine what the dimensions should be
     */
    private void dimensions(float width) {
        if (width > 1000) {
            DY = 100 * 2; // the vertical spacing between levels
            PAD = 40 * 2; // left/right margins - modifies WIDTH
            MRS = 25 * 2; // minimum regular separation
            MES = 131 * 2; // minimum expandable separation
            Y0 = 30 * 2; // depth of pendant
            Y1 = 23 * 2; // depth of pendant
            Y2 = 32 * 2; // depth of pendant
            Y3 = 16 * 2; // height of line
            TXT = 30; // text size
            TXY = 25 * 2; // y-offset for writing label
            BW = 5 * 2; // half the box width - for the red box at labelled vertices
            LW = 3 * 2; //line width
            CHFACTOR = 4 * 1.5F;
        }
    }

    /**
     * use the list of l1,l2, ...encoded data to produce a tree
     * each li represents a row of data, with format
     * li = data&data&data separating different strips_i or format_i
     * format_i is one of skip|left|right
     * strip_i is either a,b,c or a,b1:b2,c1:c2 depending on whether we use singular
     * or multiple expandables in the strip
     *
     * @param mTreeData : encoded data in a list of strings
     */
    private void createTree(ArrayList<String> mTreeData) {
        int k = 0;
        for (String encoded : mTreeData) {
            if (0 == k) {
                if(encoded.contains(",")){ // num,indexAr,labelsAr
                    String [] Ar = encoded.split(",");
                    int num = Integer.parseInt(Ar[0]);
                    String[] sinAr = Ar[1].split(":");
                    int[] index = new int[sinAr.length];
                    for (int i = 0; i < sinAr.length; i++) {
                        index[i] = Integer.parseInt(sinAr[i]);
                    }
                    String[] labels = Ar[2].split(":");
                    String[] pages = Ar[3].split(":");
                    theRows = new Rows(num, index, labels, pages)
                            .endRow(); // using the array version
                } else
                    theRows = new Rows(encoded)
                            .endRow(); // this is not expected to carry address information
            } else {
                String[] encAr = encoded.split("&");
                for (String encStr : encAr) {
                    if (encStr.equals("skip")) {
                        theRows.skip();
                        continue;
                    }
                    if (encStr.equals("left")) {
                        theRows.indentLeft();
                        continue;
                    }
                    if (encStr.equals("right")) {
                        theRows.indentRight();
                        continue;
                    }
                    // else this is a .add :
                    if (encStr.contains(":")) {
                        String[] Ar = encStr.split(",");
                        int num = Integer.parseInt(Ar[0]);
                        String[] sinAr = Ar[1].split(":");
                        int[] indexAr = new int[sinAr.length];
                        for (int i = 0; i < sinAr.length; i++) {
                            indexAr[i] = Integer.parseInt(sinAr[i]);
                        }
                        String[] labelsAr = Ar[2].split(":");
                        String[] pagesAr = Ar[3].split(":");
                        theRows.add(new Strip(num, indexAr, labelsAr,pagesAr)); // using the array version
                    } else {
                        String[] Ar = encStr.split(","); // num, index, label, page
                        int num = Integer.parseInt(Ar[0]);
                        int index = Integer.parseInt(Ar[1]);
                        theRows.add(new Strip(num, index, Ar[2], Ar[3]));
                    }
                }
                theRows.endRow();
            }
            k++;
        }
    }

    public static Fragment newInstance(ArrayList<String> list) {
        TreeFragment f = new TreeFragment();
        Bundle bdl = new Bundle();
        bdl.putStringArrayList(TREE_DATA, list);
        f.setArguments(bdl);
        return f;
    }

    //********** private classes


    /**
     * two types: labelled or not, attached to a strip
     */
    public class Vert {
        private String label = ""; /* empty unless this is a 'pointed' vertex, which is only possible for 'reals' */
        private float x = 0; /* x- coordinate*/
        private float y;
        private float tdx = 0; // text delta-x - how much to shift text in the x-direction
        public String page = ""; // the associated page in Pages db

        public Vert() {}

        /**
         * each vertex knows how to draw itself V.draw(canvas)
         * @param canvas : supplied by the widget onDraw method
         */
        public void draw(Canvas canvas) {
            Paint paint = new Paint();
            //do we have a visible endpoint?
            if (!label.isEmpty()) {
                paint.setColor(Color.BLACK);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(TXT);

                // arrange for the two
                if (label.contains(";")) {
                    String[] Ar = label.split(";");
                    String top = Ar[0], bottom = Ar[1];
                    canvas.drawText(top, x+tdx, y + TXY, paint);
                    canvas.drawText(bottom, x+tdx, y + TXY + Y3, paint);
                } else {
                    canvas.drawText(label, x+tdx, y + TXY, paint);
                }
                paint.setColor(Color.RED);
                paint.setStrokeWidth(2 * BW);
                canvas.drawLine(x - BW, y + BW, x + BW, y + BW, paint);
            }
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(LW);
            // draw the edge to level above, provided we are NOT the top level!
            if (y > Y0)
                canvas.drawLine(x, y, x, y - Y1, paint);
        }
    }

    public class Strip {
        private ArrayList<Vert> vertices = new ArrayList<>(); /* this row's vertices */
        private ArrayList<Integer> labelPos = new ArrayList<>(); /* where these occur */
        float left = LEFT; //bounds
        float right = RIGHT;
        float upx = 0; /* x-coordinate we all poiunt up to */
        float y; /* common y-coordinates */
        int level = 0;

        /**
         * @param numvtcs : number of vertices in this strip
         * @param indexAr : indices of 'live' vertices
         * @param labelAr : labels of live vertices
         */
        Strip(int numvtcs, int[] indexAr, String[] labelAr, String[] pagesAr) {
            int len = indexAr.length;
            if (len != labelAr.length) return;
            int j = 0;
            for (int i = 0; i < numvtcs; i++) {
                Vert V = new Vert();
                if (j < len && i == indexAr[j]) {
                    V.label = labelAr[j];
                    V.page = pagesAr[j];
                    j++;
                }
                vertices.add(V);
            }
        }

        Strip(int numvtcs, int index, String label, String page) {
            for (int i = 0; i < numvtcs; i++) {
                Vert V = new Vert();
                if (i == index) {
                    V.label = label;
                    V.page = page;
                }
                vertices.add(V);
            }
        }

        public Strip setUpx(float upxvalue) {
            upx = upxvalue;  /* no need to set for each vertex? */
            return this;
        }

        public Strip setLevel(int lev, float in_y) {
            level = lev;
            y = in_y;
            for (Vert V : vertices) {
                V.y = y;
            }
            return this;
        }

        /**
         * translate position of strip  by dx horizontally
         *
         * @param dx : amount to translate by
         * @return : this same strip, so we can tandem
         */
        public Strip moveBy(float dx) {
            for (Vert V : vertices) {
                V.x += dx;
            }
            left += dx;
            right += dx;
            return this;
        }

        public float getWidth() {
            return right - left;
        }

        public void draw(Canvas canvas) {
            if (vertices.size() > 0) {
                for (Vert V : vertices) {
                    V.draw(canvas);
                }
            }
            //followed by the cap
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(LW);
            canvas.drawLine(left, y - Y1, right, y - Y1, paint);
            //line to parent - provided possible
            if (y - DY + Y2 + Y3 > 0)
                canvas.drawLine(upx, y - Y1, upx, y - DY + Y2 + Y3, paint);
        }

        /**
         * assign x-positions for each vert in vertices
         * @param lf : the start position
         */
        public void placeVerts(float lf) {
            //are we at the top?
            if (vertices.size() == 1) {
                Vert V = vertices.get(0);
                left=right=V.x = (LEFT + RIGHT) / 2;
                labelPos.add(0);
                return;
            }
            int i = 0;  /* fill labelPos using this index */
            for (Vert V : vertices) {
                if (!V.label.isEmpty()) labelPos.add(i);
                i++;
            }
            // use MRS, also set right
            int k = 0;
            left = lf;
            for (Vert V : vertices) {
                right = V.x = lf + k * MRS;
                k++;
            }
            // next we extend the widths - only if more than one expandable
            if (labelPos.size() > 1) {
                k = 0;
                int numLabels = labelPos.size();
                int num = vertices.size();
                float factor = 0; // to bloat by
                float[] delta = new float[num];
                for (i = 0; i < num; i++) {
                    delta[i] = factor;
                    if (k < numLabels && i == labelPos.get(k)) {
                        if (k + 1 == numLabels) {
                            factor = 0;
                        } else {
                            int gap = labelPos.get(k + 1) - labelPos.get(k);
                            if (gap * MRS < MES) {
                                factor = MES / gap;
                            } else {
                                factor = 0;
                            }
                        }
                        k++; // look at the next labelPos
                    }
                }
                //add
                k = 0;
                float shift = 0;
                for (Vert V : vertices) {
                    shift += delta[k++];
                    V.x += shift;
                }
                // also change the boundaries ...
                left += delta[0];
                right += shift;
            }
        }

        /**
         * @return the amount of moveBy required to set the COG of xs's in the middle
         */
        private float setAveZero() {
            float shift = 0, mid = (LEFT + RIGHT) / 2;

            for (int i : labelPos) {
                Vert V = vertices.get(i);
                shift += (mid - V.x);
            }
            return shift / labelPos.size();
        }
    }


    public class Rows {
        private ArrayList<Strip> strips = new ArrayList<>(); /* this row's 'vertices' */
        private  ArrayList<Strip> myStrips;
        private ArrayList<Float> xs = new ArrayList<>();
        private int level = 0;
        private int xptr = 0; /* how many xs's consumed */
        private float current_y = 0;

        /**
         * constructor that produces a row of >1 items
         * @param numvtcs : number of vectors in total
         * @param indexAr : array of indices of labeled vertices
         * @param labelAr : associated labels
         * @param pagesAr : associated pages
         */
        public Rows(int numvtcs, int[] indexAr, String[] labelAr, String [] pagesAr) {
            current_y = Y0+5;
            Strip S = new Strip(numvtcs, indexAr, labelAr, pagesAr)
                    .setLevel(0, current_y);
            strips.add(S);
        }

        /**
         * constructor with sole root
         * @param label : root label
         */
        public Rows(String label) {
            current_y = Y0;
            Strip S = new Strip(1, 0, label, "")   // we call this one with an empty page
                    .setLevel(0, current_y);
            strips.add(S);
        }

        /**
         * called at the end of each row added. clean up
         * @return : this, for tandem calls
         */
        public Rows endRow() {
            //isolate ours
            myStrips = new ArrayList<>();
            for (Strip S : strips) {
                if (S.level == level) myStrips.add(S);
            }
            //assign the x-coordinates of each strip S
            float left_edge = 0; // LEFT??
            for (Strip S : myStrips) {
                //place our vertices, establish left <-- left_edge ++
                S.placeVerts(left_edge);
                left_edge += S.getWidth() + MRS;
            }
            //fix the relative positions of strips. average for solitaries.
            //take separations and xs' into account for more
            arrangeStrips(); // see below
            //correct text
            fixTexts();
            //replace listing xs with the x-coordinates for the next level - positions of labelled vertices
            xs = null;
            xptr = 0;
            for (Strip S : myStrips) {
                ArrayList<Float> newxs = new ArrayList<>();
                for (Vert V : S.vertices) {
                    if (!V.label.isEmpty()) {
                        newxs.add(V.x);
                    }
                }
                if (null == xs) {
                    xs = newxs;
                } else xs.addAll(newxs);
            }
            current_y += DY;
            level++;
            return this;
        }

        /**
         * reposition the texts so that they do not overlap or move outside the visible area
         */
        private void fixTexts() {
            float leftBound = 0; // we should not meet this
            for (Strip S : myStrips) {
                for (Vert V: S.vertices){
                    if(V.label.isEmpty()) continue;
                    CharSequence lb = V.label;
                    float dx = lb.length()*CHFACTOR;
                    if( V.x-dx < leftBound ){ // only here d we shift the text
                        V.tdx = leftBound -V.x+dx + PAD/3;
                    }
                    leftBound = V.x + V.tdx + dx ;
                }
            }
        }



        /**
         * list optional arrangements taking into account the values in xs
         * create  arrays lower[], upper[] of possible positions of the left endpoint of each
         * strip in strips at the given level. the lower[] considers the values xs and a minimal
         * separation of MES between adjacent strips. fix positions by choosing to moveBy S
         * by the average of the two.
         */
        private void arrangeStrips() {
            // only one strip?
            if (myStrips.size() == 1) {
                Strip S0 = myStrips.get(0);
                S0.moveBy(S0.setAveZero());
                S0.left = S0.vertices.get(0).x;
                S0.right = S0.vertices.get(S0.vertices.size()-1).x;
                if (null==xs || xs.size()==0)return; // in case this is level 0
            }
            // ok, more ...
            float[] lower = new float[myStrips.size()];
            float L = LEFT; //+PAD;
            for (int k = 0; k < myStrips.size(); k++) {
                Strip S = myStrips.get(k);
                float width = S.getWidth();
                float Lopt = xs.get(k) - width;
                lower[k] = L < Lopt ? Lopt : L;
                L = lower[k] + width + MES;
            }
            float[] upper = new float[myStrips.size()];
            float R = RIGHT; //-PAD;
            for (int k = myStrips.size() - 1; k > -1; k--) {
                Strip S = myStrips.get(k);
                float width = S.getWidth();
                float Ropt = xs.get(k) + width;
                upper[k] = R > Ropt ? Ropt : R;
                R = upper[k] - width - MES;
            }
            for (int k = 0; k < myStrips.size(); k++) {
                Strip S = myStrips.get(k);
                float ave = (lower[k] + upper[k]) / 2;
                S.moveBy(ave - (S.right + S.left) / 2);
                // this may still be inadequate, fix it ..
                float dx = S.left - S.upx;
                if (dx > 0) {
                    S.moveBy(-dx);
                } else {
                    dx = S.upx - S.right;
                    if (dx > 0) {
                        S.moveBy(dx);
                    }
                }
            }
        }

        void draw(Canvas canvas) {
            for (Strip S : strips) {
                S.draw(canvas);
            }
        }

        /**
         * move to the next xs by shifting xptr up
         * @return : this
         */
        public Rows skip() {
            xptr++;
            return this;
        }

        public Rows indentLeft() {
            LEFT+=(RIGHT-LEFT)/4;
            return this;
        }

        public Rows indentRight() {
            RIGHT -=(RIGHT-LEFT)/4;
            return this;
        }

        public Rows add(Strip S) {
            if (xptr >= xs.size()) return this; /* too many for this level */
            S.setUpx(xs.get(xptr));
            xptr++;
            strips.add(S.setLevel(level, current_y));
            return this;
        }

        /**
         * jump to the page owned by the closest labelled vertex to (x,y)
         * @param x : x-coordinate
         * @param y : y-coordinate
         */
        public void goTo(float x, float y) {
            out:
            for(Strip S : strips) {
                for (Vert V : S.vertices) {
                    if (V.label.isEmpty()) continue ;
                    if (V.page.isEmpty()) continue;  // this is what we really check for
                    if (Math.abs(V.x-x)<50 && Math.abs(V.y-y)<50){
                        mCallback.toPage(V.page);
                        break out;
                    }
                }
            }
        }
    }


    public class TreeView extends View {
        private Rows theRows;
        private int theWidth, theHeight;

        public TreeView(Context context, Rows theRows, int theWidth, int theHeight) {
            super(context);
            this.theRows = theRows;
            this.theHeight = theHeight;
            this.theWidth = theWidth;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(theWidth, theHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);
            theRows.draw(canvas);
        }
    }
}




