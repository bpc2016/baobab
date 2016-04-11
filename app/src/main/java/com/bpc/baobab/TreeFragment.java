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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by hp on 3/22/2016.
 */
public class TreeFragment extends Fragment {
    public static final String MEMBER_ID = "MEMBER ID";
    public static final String SOURCE_ID = "SOURCE";
    public static final String TREE_DATA = "TREE DATA";

    //*********** constants
    private static final float NON_POINT = -1; /* indiates this is not a 'real' vertex endpoint */
    private static final float Y0 = 30; // depth of row_0
    private float GAP_RATIO = 0.5f;  // position of endpoint w.r.t. gap between x values 0 ... 0.99

    private float DY = 100; // the vertical spacing between levels
    private float PAD = 20; // left/right margins - modifes WIDTH
    private float MRS = 30; // minimum regular separation
    private float MES = 125; // minimum expandable separation
    private float Y1 = 23; // depth of pendant
    private float Y2 = 32; // depth of pendant
    private float Y3 = 16; // height of line
    private float TXT = 16; // text size
    private float TXY = 25; // y-offset for writing label
    private float BW = 5; // half the box width - for teh red box at labelled vertices
    private float LW = 3; //line width

    private static final String LOGGER = "bpc_tree"; //Log.d(LOGGER, "page = " + real_id + ", member = " + member_id);

    //************* vars
    private Rows theRows;
    private float LEFT, RIGHT;
    private ArrayList<String> mTreeData;

    public TreeFragment() {
    } // generic constructor

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

        Log.d(LOGGER, "width " + WIDTH);

        dimensions(WIDTH);

        RIGHT = WIDTH - PAD;
        LEFT = PAD;

        createTree(mTreeData);

        LinearLayout v1 = (LinearLayout) v.findViewById(R.id.tree_material);

        v1.addView(new TreeView(getActivity(), theRows, WIDTH, HEIGHT));
        return v;
    }

    /**
     * depending on width, determine what the dimensions should be
     */
    private void dimensions(float width) {
        if (width > 1000) {
            DY = 100 * 2; // the vertical spacing between levels
            PAD = 20 * 2; // left/right margins - modifes WIDTH
            MRS = 30 * 2; // minimum regular separation
            MES = 125 * 2; // minimum expandable separation
            Y1 = 23 * 2; // depth of pendant
            Y2 = 32 * 2; // depth of pendant
            Y3 = 16 * 2; // height of line
            TXT = 30; // text size
            TXY = 25 * 2; // y-offset for writing label
            BW = 5 * 2; // half the box width - for teh red box at labelled vertices
            LW = 3 * 2; //line width
        }
    }


    /**
     * use the list of encoded data to produce a tree
     * string represents a row of data
     * string is data&data&data seprating different strips
     * strip is either a,b,c or a,b1:b2,c1:c2 depending on whether we use singular
     * or multiple expandables in the strip
     *
     * @param mTreeData : encoded data in a list of strings
     */
    private void createTree(ArrayList<String> mTreeData) {
        int k = 0;
        for (String encoded : mTreeData) {
            if (0 == k) {
                theRows = new Rows(encoded)
                        .endRow();
            } else {
                String [] encAr = encoded.split("&");
                for(String encStr: encAr) {
                    if(encStr.equals("skip")){
                        theRows.skip();
                    } else if (encStr.contains(":")){
                        String[] Ar = encStr.split(",");
                        int num = Integer.parseInt(Ar[0]);
                        String [] sinAr = Ar[1].split(":");
                        int [] index = new int [sinAr.length];
                        for(int i=0;i<sinAr.length;i++){
                            index[i] = Integer.parseInt(sinAr[i]);
                        }
                        String [] labels = Ar[2].split(":");
                        theRows.add(new Strip(num, index, labels)); // using the array version
                    } else {
                        String[] Ar = encStr.split(",");
                        int num = Integer.parseInt(Ar[0]);
                        int index = Integer.parseInt(Ar[1]);
                        theRows.add(new Strip(num, index, Ar[2]));
                    }
                }
                theRows.endRow();
            }
            k++;
        }
    }

    //Log.d(LOGGER, "i: "+T.parents(m) +" >> "+m.getSiblings());

    //      }

//        Log.d(LOGGER, "logs"); //use crumbs last index is: " + Leaves.get(0).print() + " .. "+ T.NodeAt(Leaves.get(0).getIndex())  );
//        Toast.makeText(getActivity(),
//                "tree done, size is: " + T.getCount(), Toast.LENGTH_LONG).show();

/*
        theRows = new Rows("home base")
                .endRow()
                .add(new Strip(7, new int[]{0, 3, 4}, new String[]{"first guy", "then another", "third one"}))
//                .add(new Strip(7, new int[]{3, 4}, new String[]{"first guy", "then another"}))
                .endRow()
                .add(new Strip(4, 3, "member 3"))
                .add(new Strip(4, 0, "member four"))
                .add(new Strip(2, 1, "member five"))
                .endRow()
                .add(new Strip(4, 3, "member 3"))
                .add(new Strip(5, new int[]{0, 4}, new String[]{"first guy", "then another"}))
                .add(new Strip(4, 2, "member five"))
                .endRow()
        ;*/


/*    public static TreeFragment newInstance(String member_id, int source_id) {
        TreeFragment f = new TreeFragment();
        Bundle bdl = new Bundle();
        bdl.putString(MEMBER_ID, member_id);
        bdl.putInt(SOURCE_ID, source_id);
        f.setArguments(bdl);
        return f;
    }*/

    public static Fragment newInstance(ArrayList<String> list) {
        TreeFragment f = new TreeFragment();
        Bundle bdl = new Bundle();
        bdl.putStringArrayList(TREE_DATA, list);
        f.setArguments(bdl);
        for(String s: list){
            Log.d(LOGGER, "K: " + s);
        }
        return f;
    }


    //********** private classes


    /**
     * two types: labelled or not, attached to a strip
     */
    public class Vert {
        private String label = ""; /* empty unless this is a 'pointed' vertex, which is only possible for 'reals' */
        private float x = 0; /* x- coordinate*/
        public float y;
        public int strip; /* each vertex belongs to a strip of siblings */

        public Vert() {
        }

        /**
         * each vertex knows how to draw itself V.draw(canvas)
         *
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
                    canvas.drawText(top, x, y + TXY, paint);
                    canvas.drawText(bottom, x, y + TXY + Y3, paint);
                } else {
                    canvas.drawText(label, x, y + TXY, paint);
                }


                paint.setColor(Color.RED);
                paint.setStrokeWidth(2 * BW);
                canvas.drawLine(x - BW, y + BW, x + BW, y + BW, paint);
            }
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(LW);
            // draw the edge to level above, provided we are NOT the top level!
            if (y - DY > 0)
                canvas.drawLine(x, y, x, y - Y1, paint);
        }
    }

    public class Strip {
        private ArrayList<Vert> vertices = new ArrayList<>(); /* this row's vertices */
        private ArrayList<Integer> xpos = new ArrayList<>(); /* where these occur */
        //bounds
        float left = LEFT, L;
        float right = RIGHT, R;
        //x-coordinate above
        float upx = 0; /* x-coordinate we all poiunt up to */
        float y; /* common y-coordinates */
        int level = 0;
        boolean empty = true;  /* is this an empty strip? default is yes*/

        /**
         * @param numvtcs : number of vertices in this strip
         * @param indexAr : indices of 'live' vertices
         * @param labelAr : labels of live vertices
         */
        Strip(int numvtcs, int[] indexAr, String[] labelAr) {
            int len = indexAr.length;
            if (len != labelAr.length) return;
            int j = 0;
            for (int i = 0; i < numvtcs; i++) {
                Vert V = new Vert();
                if (j < len && i == indexAr[j]) {
                    V.label = labelAr[j];
                    j++;
                }
                vertices.add(V);
            }
            empty = false;
        }

        Strip(int numvtcs, int index, String label) {
            for (int i = 0; i < numvtcs; i++) {
                Vert V = new Vert();
                if (i == index) {
                    V.label = label;
                }
                vertices.add(V);
            }
            empty = false; /*default non-empty*/
        }

        Strip() {
        } //blank strip

        public boolean isEmpty() {
            return empty;
        }

        public Strip setUpx(float upxvalue) {
            upx = upxvalue;  /* no need to set for each vertex? */
            return this;
        }

        public Strip setLevel(int lev) {
            level = lev;
            y = Y0 + lev * DY;
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
        public Strip shift(float dx) {
            for (Vert V : vertices) {
                V.x += dx;
            }
            left += dx;
            right += dx;
            return this;
        }

        public void draw(Canvas canvas) {
            if (empty) return;
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
            //line to parent
            canvas.drawLine(upx, y - Y1, upx, y - DY + Y2 + Y3, paint);
        }

        public void placeVerts() {
            float xx = left;
            //are we at the top?
            if (vertices.size() == 1) {
                xx = (left + right) / 2;
                Vert V = vertices.get(0);
                V.x = xx;
                xpos.add(0);
            }
            //linearly set the vertices - start with the margins, left and right
            xx = left;
            int i = 0;  /* fill xpos using this index */
            float dx = (right - left) / (vertices.size() - 1);
            for (Vert V : vertices) {
                V.x = xx;
                if (!V.label.isEmpty()) {
                    xpos.add(i);
                }
                xx += dx;
                i++;
            }
            // fix gaps
            narrowGaps(); // modify gaps if necessary
        }


        private void narrowGaps() {
            int num = vertices.size();
            int i;

            int k = 0;
            for (Vert V : vertices) {
                V.x = left + k * MRS;
                k++;
            }

            // next we extend the widths - only if more than one expandable
            if (xpos.size() > 1) {
                k = 0;
                int numxpos = xpos.size();
                float factor = 0; // to bloat by
                float[] delta = new float[num];
                for (i = 0; i < num; i++) {
                    delta[i] = factor;
                    if (k < numxpos && i == xpos.get(k)) {
                        if (k + 1 == numxpos) {
                            factor = 0;
                        } else {
                            int gap = xpos.get(k + 1) - xpos.get(k);
                            if (gap * MRS < MES) {
                                factor = MES / gap;
                            } else {
                                factor = 0;
                            }
                        }
                        k++; // look at the next xpos
                    }
                }
                //add
                k = 0;
                float shift = 0;
                for (Vert V : vertices) {
                    shift += delta[k++];
                    V.x += shift;
                }
            }

            float new_l = vertices.get(0).x; //left
            float new_r = vertices.get(num - 1).x; // was V

            float wd = new_r - new_l;
            float shift_amount = ((right - left) - wd) / 2;

            k = 0;
            for (Vert V : vertices) {
                V.x += shift_amount;
            }
            left = new_l + shift_amount;
            right = new_r + shift_amount;
        }
    }


    public class Rows {
        private ArrayList<Strip> strips = new ArrayList<>(); /* this row's 'vertices' */
        private ArrayList<Float> xs = new ArrayList<>();
        private int level = 0;
        private int xptr = 0; /* how many xs's consumed */
        private float ratio = GAP_RATIO;

        public Rows(String label) {
            Strip S = new Strip(1, 0, label)
                    .setLevel(0);
            S.left = S.right = (LEFT + RIGHT) / 2;
            strips.add(S);
        }

        public Rows endRow() {
            xptr = 0;
            //count how many, how many empty
            int empty = 0, all = 0;
            for (Strip S : strips) {
                if (S.level != level) continue;
                if (S.isEmpty()) empty++;
                all++;
            }
            /**
             * this section handles empty strips in the call: 'Strip()'
             * and decides the sharing of endpoints for strips
             */
            //decide 'empty' width - 4:1 full:empty
            float ewidth = (RIGHT - LEFT) / (4 * (all - empty) + empty);
            //set left/right boundaries
            float ll = LEFT; //,rr;
            boolean onEmpty = true;
            int emptyRight = empty;
            for (Strip S : strips) {
                if (level == 0) continue;  //skip the top
                if (S.level != level) continue;
                S.left = ll;
                if (onEmpty) {
                    if (S.isEmpty()) {
                        ll = S.right = ll + ewidth;
                        if (emptyRight > 0) emptyRight--;
                    } else {
                        onEmpty = false;
                        if (xptr == xs.size() - 1) { // last strip
                            S.right = RIGHT - ewidth * emptyRight;
                        } else {
                            ll = nextLeft(S); // also increments xptr
                        }
                    }
                } else {
                    if (S.isEmpty()) {
                        ll = S.right = ll + ewidth;
                        if (emptyRight > 0) emptyRight--;
                        onEmpty = true;
                    } else {
                        if (xptr == xs.size() - 1) { // last strip
                            S.right = RIGHT - ewidth * emptyRight;
                        } else {
                            ll = nextLeft(S);  // also increments xptr
                        }
                    }
                }
            }
            /**
             * assign the x-coordinates of each strip S
             */
            for (Strip S : strips) {
                if (S.level != level) continue;
                S.L = S.left;
                S.R = S.right; // keep prev values
                S.placeVerts();
            }

            /**
             * modify one last time - align verticals to expandable above
             */
            alignAll();

            /**
             * replace listing xs with the x-coordinates for the next level - positions of labelled vertices
             */
            xs = null;
            xptr = 0;
            for (Strip S : strips) {
                if (S.level != level) continue;
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

            level++;

            //debugging
//            String res = "";
//            for (float re : xs) {
//                res += "," + re;
//            }
//            Log.d(LOGGER, "endRow : xs=" + res);
            return this;
        }

        /**
         * compare the left/right endpoints of each strip against the x-coordinate
         * of its assoiated labelled ancestor. shift to the edge if outside, else keep as is
         */
        private void alignAll() {
            if (level == 0) return;
            for (Strip S : strips) {
                if (level != S.level) continue;
                if (S.upx > S.right) {
                    float shift = S.upx - S.right; //(S.right+S.left)/2;
                    for (Vert V : S.vertices) {
                        V.x += shift;
                    }
                    S.left += shift;
                    S.right += shift;
                }
                if (S.upx < S.left) {
                    float shift = //(S.right+S.left)/2
                            S.left - S.upx;
                    for (Vert V : S.vertices) {
                        V.x -= shift;
                    }
                    S.left -= shift;
                    S.right -= shift;
    Log.d(LOGGER, "align : LL=" + S.left);
                }
            }
        }

        /**
         * determine the x-coordinate of the next strip's left end
         * depends on the x values of exapnadables above, and uses this Rows object ratio 0 < ratio < 1
         *
         * @param S : strip
         * @return the desired value for S*.left
         */
        private float nextLeft(Strip S) {
            float ratio = this.ratio * 0.5f;
            float ll, nr;
            nr = S.right = (1 - ratio) * xs.get(xptr) + ratio * xs.get(xptr + 1);
            ll = ratio * xs.get(xptr) + (1 - ratio) * xs.get(xptr + 1);
            xptr++;
//            Log.d(LOGGER, "nextLeft : ll=" + ll + ", nr=" + nr);
            return ll;
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
        public Rows skip(){
            xptr++;
            return this;
        }

        public Rows add(Strip S) {
            if (!S.isEmpty()) {
                if (xptr >= xs.size()) return this; /* too many for this level */
                S.setUpx(xs.get(xptr));
                xptr++;
            }
            strips.add(S.setLevel(level));
            return this;
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
            setMeasuredDimension(theWidth, theHeight); //(576, 822); // setMeasuredDimension(100, 100);
//        setMeasuredDimension( getSuggestedMinimumWidth(),getSuggestedMinimumHeight());
        }


        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);
            theRows.draw(canvas);

        }

    }
}
