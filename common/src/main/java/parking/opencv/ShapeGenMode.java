
package parking.opencv;
// Mode used to find shape
public enum ShapeGenMode {
	EDGESCAN,        		// find connected canny edges
	BINBORDER,       		// find connected edges in binary image
	BINREGIONRECURSIVE,		// recursively grow connected regions in binary image
	BINREGION               // grow connected regions in binary image
}
