#include "tango-augmented-reality/convex_hull.h"


namespace tango_augmented_reality {

    double ConvexHull::isLeft(p2t::Point P0, p2t::Point P1, p2t::Point P2) {
        return (P1.x - P0.x) * (P2.y - P0.y) - (P2.x - P0.x) * (P1.y - P0.y);
    }

    std::vector <p2t::Point> ConvexHull::generateConvexHull(std::vector < p2t::Point > &P) {
        // http://geomalgorithms.com/a10-_hull-1.html

        int bot = 0;
        int top = (-1);
        int i;
        int n = P.size();

        std::vector <p2t::Point> H;
        H.resize(P.size());

        int minmin = 0, minmax;
        float xmin = P[0].x;
        for (i = 1; i < n; i++)
            if (P[i].x != xmin) break;
        minmax = i - 1;
        if (minmax == n - 1) {
            H[++top] = P[minmin];
            if (P[minmax].y != P[minmin].y)
                H[++top] = P[minmax];
            H[++top] = P[minmin];
            H.resize(top + 1);
            return H;
        }

        int maxmin, maxmax = n - 1;
        float xmax = P[n - 1].x;
        for (i = n - 2; i >= 0; i--)
            if (P[i].x != xmax) break;
        maxmin = i + 1;

        H[++top] = P[minmin];
        i = minmax;
        while (++i <= maxmin) {
            if (isLeft(P[minmin], P[maxmin], P[i]) >= 0 && i < maxmin)
                continue;
            while (top > 0) {
                if (isLeft(H[top - 1], H[top], P[i]) > 0)
                    break;
                else
                    top--;
            }
            H[++top] = P[i];
        }

        if (maxmax != maxmin)
            H[++top] = P[maxmax];
        bot = top;
        i = maxmin;
        while (--i >= minmax) {
            if (isLeft(P[maxmax], P[minmax], P[i]) >= 0 && i > minmax)
                continue;
            while (top > bot) {
                if (isLeft(H[top - 1], H[top], P[i]) > 0)
                    break;
                else
                    top--;
            }
            H[++top] = P[i];
        }
        if (minmax != minmin)
            H[++top] = P[minmin];

        H.resize(top + 1);
        return H;
    }
}