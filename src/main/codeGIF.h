

#include <stdio.h>   // for FILE*
#include <string.h>  // for memcpy and bzero

// Define these macros to hook into a custom memory allocator.
// TEMP_MALLOC and TEMP_FREE will only be called in stack fashion - frees in the reverse order of mallocs
// and any temp memory allocated by a function will be freed before it exits.
// MALLOC and FREE are used only by GifBegin and GifEnd respectively (to allocate a buffer the size of the image, which
// is used to find changed pixels for delta-encoding.)

typedef unsigned char uchar;

const int kGifTransIndex = 0;
const int errorThres=10;

struct GifPalette
{
	int bitDepth;

	uchar r[256];
	uchar g[256];
	uchar b[256];

	// k-d tree over RGB space, organized in heap fashion
	// i.e. left child of node i is node i*2, right child is node i*2+1
	// nodes 256-511 are implicitly the leaves, containing a color
	uchar treeSplitElt[255];
	uchar treeSplit[255];

	uchar neighborIndex[256][4];    // 4�������ɫ��index
};

// max, min, and abs functions
int GifIMax(int l, int r) { return l>r?l:r; }
int GifIMin(int l, int r) { return l<r?l:r; }
int GifIAbs(int i) { return i<0?-i:i; }

// walks the k-d tree to pick the palette entry for a desired color.
// Takes as in/out parameters the current best color and its error -
// only changes them if it finds a better color in its subtree.
// this is the major hotspot in the code at the moment.
void GifGetClosestPaletteColor(GifPalette* pPal, int r, int g, int b, int& bestInd, int& bestDiff, int treeRoot = 1)
{
	// base case, reached the bottom of the tree
	if(treeRoot > (1<<pPal->bitDepth)-1)
	{
		int ind = treeRoot-(1<<pPal->bitDepth);
		if(ind == kGifTransIndex) return;

		// check whether this color is better than the current winner
		int r_err = r - ((int)pPal->r[ind]);
		int g_err = g - ((int)pPal->g[ind]);
		int b_err = b - ((int)pPal->b[ind]);
		int diff = GifIAbs(r_err)+GifIAbs(g_err)+GifIAbs(b_err);

		if(diff < bestDiff)
		{
			bestInd = ind;
			bestDiff = diff;
		}

		return;
	}

	// take the appropriate color (r, g, or b) for this node of the k-d tree
	int comps[3]; comps[0] = r; comps[1] = g; comps[2] = b;
	int splitComp = comps[pPal->treeSplitElt[treeRoot]];

	int splitPos = pPal->treeSplit[treeRoot];
	if(splitPos > splitComp)
	{
		// check the left subtree
		GifGetClosestPaletteColor(pPal, r, g, b, bestInd, bestDiff, treeRoot*2);
		if( bestDiff > splitPos - splitComp )
		{
			// cannot prove there's not a better value in the right subtree, check that too
			GifGetClosestPaletteColor(pPal, r, g, b, bestInd, bestDiff, treeRoot*2+1);
		}
	}
	else
	{
		GifGetClosestPaletteColor(pPal, r, g, b, bestInd, bestDiff, treeRoot*2+1);
		if( bestDiff > splitComp - splitPos )
		{
			GifGetClosestPaletteColor(pPal, r, g, b, bestInd, bestDiff, treeRoot*2);
		}
	}
}

void GifSwapPixels(uchar* image, int pixA, int pixB)
{
	uchar rA = image[pixA*4];
	uchar gA = image[pixA*4+1];
	uchar bA = image[pixA*4+2];
	uchar aA = image[pixA*4+3];

	uchar rB = image[pixB*4];
	uchar gB = image[pixB*4+1];
	uchar bB = image[pixB*4+2];
	uchar aB = image[pixA*4+3];

	image[pixA*4] = rB;
	image[pixA*4+1] = gB;
	image[pixA*4+2] = bB;
	image[pixA*4+3] = aB;

	image[pixB*4] = rA;
	image[pixB*4+1] = gA;
	image[pixB*4+2] = bA;
	image[pixB*4+3] = aA;
}

// just the partition operation from quicksort
int GifPartition(uchar* image, const int left, const int right, const int elt, int pivotIndex)
{
	const int pivotValue = image[(pivotIndex)*4+elt];
	GifSwapPixels(image, pivotIndex, right-1);
	int storeIndex = left;
	bool split = 0;
	for(int ii=left; ii<right-1; ++ii)
	{
		int arrayVal = image[ii*4+elt];
		if( arrayVal < pivotValue )
		{
			GifSwapPixels(image, ii, storeIndex);
			++storeIndex;
		}
		else if( arrayVal == pivotValue )
		{
			if(split)
			{
				GifSwapPixels(image, ii, storeIndex);
				++storeIndex;
			}
			split = !split;
		}
	}
	GifSwapPixels(image, storeIndex, right-1);
	return storeIndex;
}

// Perform an incomplete sort, finding all elements above and below the desired median
void GifPartitionByMedian(uchar* image, int left, int right, int com, int neededCenter)
{
	if(left < right-1)
	{
		int pivotIndex = left + (right-left)/2;

		pivotIndex = GifPartition(image, left, right, com, pivotIndex);

		// Only "sort" the section of the array that contains the median
		if(pivotIndex > neededCenter)
			GifPartitionByMedian(image, left, pivotIndex, com, neededCenter);

		if(pivotIndex < neededCenter)
			GifPartitionByMedian(image, pivotIndex+1, right, com, neededCenter);
	}
}

// Builds a palette by creating a balanced k-d tree of all pixels in the image
void GifSplitPalette(uchar* image, int numPixels, int firstElt, int lastElt, int splitElt, int splitDist, int treeNode, bool buildForDither, GifPalette* pal)
{
	if(lastElt <= firstElt || numPixels == 0)
		return;

	// base case, bottom of the tree
	if(lastElt == firstElt+1)
	{
		if(buildForDither)
		{
			// Dithering needs at least one color as dark as anything
			// in the image and at least one brightest color -
			// otherwise it builds up error and produces strange artifacts
			if( firstElt == 1 )
			{
				// special case: the darkest color in the image
				int r=255, g=255, b=255;
				for(int ii=0; ii<numPixels; ++ii)
				{
					r = GifIMin(r, image[ii*4+0]);
					g = GifIMin(g, image[ii*4+1]);
					b = GifIMin(b, image[ii*4+2]);
				}

				pal->r[firstElt] = r;
				pal->g[firstElt] = g;
				pal->b[firstElt] = b;

				return;
			}

			if( firstElt == (1 << pal->bitDepth)-1 )
			{
				// special case: the lightest color in the image
				int r=0, g=0, b=0;
				for(int ii=0; ii<numPixels; ++ii)
				{
					r = GifIMax(r, image[ii*4+0]);
					g = GifIMax(g, image[ii*4+1]);
					b = GifIMax(b, image[ii*4+2]);
				}

				pal->r[firstElt] = r;
				pal->g[firstElt] = g;
				pal->b[firstElt] = b;

				return;
			}
		}

		// otherwise, take the average of all colors in this subcube
		int r=0, g=0, b=0;
		for(int ii=0; ii<numPixels; ++ii)
		{
			r += image[ii*4+0];
			g += image[ii*4+1];
			b += image[ii*4+2];
		}

		r += numPixels / 2;  // round to nearest
		g += numPixels / 2;
		b += numPixels / 2;

		r /= numPixels;
		g /= numPixels;
		b /= numPixels;

		pal->r[firstElt] = (uchar)r;
		pal->g[firstElt] = (uchar)g;
		pal->b[firstElt] = (uchar)b;

		return;
	}

	// Find the axis with the largest range
	int minR = 255, maxR = 0;
	int minG = 255, maxG = 0;
	int minB = 255, maxB = 0;
	for(int ii=0; ii<numPixels; ++ii)
	{
		int r = image[ii*4+0];
		int g = image[ii*4+1];
		int b = image[ii*4+2];

		if(r > maxR) maxR = r;
		if(r < minR) minR = r;

		if(g > maxG) maxG = g;
		if(g < minG) minG = g;

		if(b > maxB) maxB = b;
		if(b < minB) minB = b;
	}

	int rRange = maxR - minR;
	int gRange = maxG - minG;
	int bRange = maxB - minB;

	// and split along that axis. (incidentally, this means this isn't a "proper" k-d tree but I don't know what else to call it)
	int splitCom = 1;
	if(bRange > gRange) splitCom = 2;
	if(rRange > bRange && rRange > gRange) splitCom = 0;

	int subPixelsA = numPixels * (splitElt - firstElt) / (lastElt - firstElt);
	int subPixelsB = numPixels-subPixelsA;

	GifPartitionByMedian(image, 0, numPixels, splitCom, subPixelsA);

	pal->treeSplitElt[treeNode] = splitCom;
	pal->treeSplit[treeNode] = image[subPixelsA*4+splitCom];

	GifSplitPalette(image,              subPixelsA, firstElt, splitElt, splitElt-splitDist, splitDist/2, treeNode*2,   buildForDither, pal);
	GifSplitPalette(image+subPixelsA*4, subPixelsB, splitElt, lastElt,  splitElt+splitDist, splitDist/2, treeNode*2+1, buildForDither, pal);
}

// Finds all pixels that have changed from the previous image and
// moves them to the fromt of th buffer.
// This allows us to build a palette optimized for the colors of the
// changed pixels only.
int GifPickChangedPixels( const uchar* lastFrame, uchar* frame, int numPixels )
{
	int numChanged = 0;
	uchar* writeIter = frame;

	for (int ii=0; ii<numPixels; ++ii)
	{
		if(lastFrame[0] != frame[0] ||
			lastFrame[1] != frame[1] ||
			lastFrame[2] != frame[2])
		{
			writeIter[0] = frame[0];
			writeIter[1] = frame[1];
			writeIter[2] = frame[2];
			++numChanged;
			writeIter += 4;
		}
		lastFrame += 4;
		frame += 4;
	}

	return numChanged;
}

// Creates a palette by placing all the image pixels in a k-d tree and then averaging the blocks at the bottom.
// This is known as the "modified median split" technique
void GifMakePalette( const uchar* lastFrame, const uchar* nextFrame, int width, int height, int bitDepth, bool buildForDither, GifPalette* pPal )
{
	pPal->bitDepth = bitDepth;

	// SplitPalette is destructive (it sorts the pixels by color) so
	// we must create a copy of the image for it to destroy
	int imageSize = width*height*4*sizeof(uchar);
	uchar* destroyableImage = (uchar*)malloc(imageSize);
	memcpy(destroyableImage, nextFrame, imageSize);

	int numPixels = width*height;
	if(lastFrame)
		numPixels = GifPickChangedPixels(lastFrame, destroyableImage, numPixels);

	const int lastElt = 1 << bitDepth;
	const int splitElt = lastElt/2;
	const int splitDist = splitElt/2;

	GifSplitPalette(destroyableImage, numPixels, 1, lastElt, splitElt, splitDist, 1, buildForDither, pPal);

	free(destroyableImage);

	// add the bottom node for the transparency index
	pPal->treeSplit[1 << (bitDepth-1)] = 0;
	pPal->treeSplitElt[1 << (bitDepth-1)] = 0;

	pPal->r[0] = pPal->g[0] = pPal->b[0] = 0;
}

// Implements Floyd-Steinberg dithering, writes palette value to alpha
void GifDitherImage( const uchar* lastFrame, const uchar* nextFrame, uchar* outFrame, int width, int height, GifPalette* pPal )
{
	int numPixels = width*height;

	// quantPixels initially holds color*256 for all pixels
	// The extra 8 bits of precision allow for sub-single-color error values
	// to be propagated
	int* quantPixels = (int*)malloc(sizeof(int)*numPixels*4);

	for( int ii=0; ii<numPixels*4; ++ii )
	{
		uchar pix = nextFrame[ii];
		int pix16 = int(pix) * 256;
		quantPixels[ii] = pix16;
	}

	for( int yy=0; yy<height; ++yy )
	{
		for( int xx=0; xx<width; ++xx )
		{
			int* nextPix = quantPixels + 4*(yy*width+xx);
			const uchar* lastPix = lastFrame? lastFrame + 4*(yy*width+xx) : NULL;

			// Compute the colors we want (rounding to nearest)
			int rr = (nextPix[0] + 127) / 256;
			int gg = (nextPix[1] + 127) / 256;
			int bb = (nextPix[2] + 127) / 256;

			// if it happens that we want the color from last frame, then just write out
			// a transparent pixel
			if( lastFrame &&
				lastPix[0] == rr &&
				lastPix[1] == gg &&
				lastPix[2] == bb )
			{
				nextPix[0] = rr;
				nextPix[1] = gg;
				nextPix[2] = bb;
				nextPix[3] = kGifTransIndex;
				continue;
			}

			int bestDiff = 1000000;
			int bestInd = kGifTransIndex;

			// Search the palete
			GifGetClosestPaletteColor(pPal, rr, gg, bb, bestInd, bestDiff);

			// Write the result to the temp buffer
			int r_err = nextPix[0] - int(pPal->r[bestInd]) * 256;
			int g_err = nextPix[1] - int(pPal->g[bestInd]) * 256;
			int b_err = nextPix[2] - int(pPal->b[bestInd]) * 256;

			nextPix[0] = pPal->r[bestInd];
			nextPix[1] = pPal->g[bestInd];
			nextPix[2] = pPal->b[bestInd];
			nextPix[3] = bestInd;

			// Propagate the error to the four adjacent locations
			// that we haven't touched yet
			int quantloc_7 = (yy*width+xx+1);
			int quantloc_3 = (yy*width+width+xx-1);
			int quantloc_5 = (yy*width+width+xx);
			int quantloc_1 = (yy*width+width+xx+1);

			if(quantloc_7 < numPixels)
			{
				int* pix7 = quantPixels+4*quantloc_7;
				pix7[0] += GifIMax( -pix7[0], r_err * 7 / 16 );
				pix7[1] += GifIMax( -pix7[1], g_err * 7 / 16 );
				pix7[2] += GifIMax( -pix7[2], b_err * 7 / 16 );
			}

			if(quantloc_3 < numPixels)
			{
				int* pix3 = quantPixels+4*quantloc_3;
				pix3[0] += GifIMax( -pix3[0], r_err * 3 / 16 );
				pix3[1] += GifIMax( -pix3[1], g_err * 3 / 16 );
				pix3[2] += GifIMax( -pix3[2], b_err * 3 / 16 );
			}

			if(quantloc_5 < numPixels)
			{
				int* pix5 = quantPixels+4*quantloc_5;
				pix5[0] += GifIMax( -pix5[0], r_err * 5 / 16 );
				pix5[1] += GifIMax( -pix5[1], g_err * 5 / 16 );
				pix5[2] += GifIMax( -pix5[2], b_err * 5 / 16 );
			}

			if(quantloc_1 < numPixels)
			{
				int* pix1 = quantPixels+4*quantloc_1;
				pix1[0] += GifIMax( -pix1[0], r_err / 16 );
				pix1[1] += GifIMax( -pix1[1], g_err / 16 );
				pix1[2] += GifIMax( -pix1[2], b_err / 16 );
			}
		}
	}

	// Copy the palettized result to the output buffer
	for( int ii=0; ii<numPixels*4; ++ii )
	{
		outFrame[ii] = quantPixels[ii];
	}

	free(quantPixels);
}

// Picks palette colors for the image using simple thresholding, no dithering
void GifThresholdImage( const uchar* lastFrame, const uchar* nextFrame, uchar* outFrame, int width, int height, GifPalette* pPal )
{
	int numPixels = width*height;
	for( int ii=0; ii<numPixels; ++ii )
	{
		// if a previous color is available, and it matches the current color,
		// set the pixel to transparent
		if(lastFrame && (abs(lastFrame[0]-nextFrame[0])+abs(lastFrame[1]-nextFrame[1])+abs(lastFrame[2]-nextFrame[2]))<0)
		{
			outFrame[0] = lastFrame[0];
			outFrame[1] = lastFrame[1];
			outFrame[2] = lastFrame[2];
			outFrame[3] = kGifTransIndex;
		}
		else
		{
			// palettize the pixel
			int bestDiff = 1000000;
			int bestInd = 1;
			GifGetClosestPaletteColor(pPal, nextFrame[0], nextFrame[1], nextFrame[2], bestInd, bestDiff);

			// Write the resulting color to the output buffer
			outFrame[0] = pPal->r[bestInd];
			outFrame[1] = pPal->g[bestInd];
			outFrame[2] = pPal->b[bestInd];
			if(outFrame[0]>100 && outFrame[0]<180)
				outFrame[0]=outFrame[0];
			outFrame[3] = bestInd;
		}

		if(lastFrame) lastFrame += 4;
		outFrame += 4;
		nextFrame += 4;
	}
}

// Simple structure to write out the LZW-compressed portion of the image
// one bit at a time
struct GifBitStatus
{
	uchar bitIndex;  // how many bits in the partial byte written so far
	uchar byte;      // current partial byte

	int chunkIndex;
	uchar chunk[256];   // bytes are written in here until we have 256 of them, then written to the file
};

// insert a single bit
void GifWriteBit( GifBitStatus& stat, int bit )
{
	bit = bit & 1;
	bit = bit << stat.bitIndex;
	stat.byte |= bit;

	++stat.bitIndex;
	if( stat.bitIndex > 7 )
	{
		// move the newly-finished byte to the chunk buffer 
		stat.chunk[stat.chunkIndex++] = stat.byte;
		// and start a new byte
		stat.bitIndex = 0;
		stat.byte = 0;
	}
}

// write all bytes so far to the file
void GifWriteChunk( FILE* f, GifBitStatus& stat )
{
	fputc(stat.chunkIndex, f);
	fwrite(stat.chunk, 1, stat.chunkIndex, f);

	stat.bitIndex = 0;
	stat.byte = 0;
	stat.chunkIndex = 0;
}

void GifWriteCode( FILE* f, GifBitStatus& stat, int code, int length )
{
	for( int ii=0; ii<length; ++ii )
	{
		GifWriteBit(stat, code);
		code = code >> 1;

		if( stat.chunkIndex == 255 )
		{
			GifWriteChunk(f, stat);
		}
	}
}

// The LZW dictionary is a 256-ary tree constructed as the file is encoded,
// this is one node
struct GifLzwNode
{
	int m_next[256];
};

// write a 256-color (8-bit) image palette to the file
void GifWritePalette( const GifPalette* pPal, FILE* f )
{
	fputc(0, f);  // first color: transparency
	fputc(0, f);
	fputc(0, f);

	for(int ii=1; ii<(1 << pPal->bitDepth); ++ii)
	{
		int r = pPal->r[ii];
		int g = pPal->g[ii];
		int b = pPal->b[ii];

		fputc(r, f);
		fputc(g, f);
		fputc(b, f);
	}
}

// write the image header, LZW-compress and write out the image
void GifWriteLzwImage(FILE* f, uchar* image, int left, int top,  int width, int height, int delay, GifPalette* pPal)
{
	// graphics control extension
	fputc(0x21, f);
	fputc(0xf9, f);
	fputc(0x04, f);
	fputc(0x05, f); // leave prev frame in place, this frame has transparency
	fputc(delay & 0xff, f);
	fputc((delay >> 8) & 0xff, f);
	fputc(kGifTransIndex, f); // transparent color index
	fputc(0, f);

	fputc(0x2c, f); // image descriptor block

	fputc(left & 0xff, f);           // corner of image in canvas space
	fputc((left >> 8) & 0xff, f);
	fputc(top & 0xff, f);
	fputc((top >> 8) & 0xff, f);

	fputc(width & 0xff, f);          // width and height of image
	fputc((width >> 8) & 0xff, f);
	fputc(height & 0xff, f);
	fputc((height >> 8) & 0xff, f);

	//fputc(0, f); // no local color table, no transparency
	//fputc(0x80, f); // no local color table, but transparency

	fputc(0x80 + pPal->bitDepth-1, f); // local color table present, 2 ^ bitDepth entries
	GifWritePalette(pPal, f);

	const int minCodeSize = pPal->bitDepth;
	const int clearCode = 1 << pPal->bitDepth;

	fputc(minCodeSize, f); // min code size 8 bits

	GifLzwNode* codetree = (GifLzwNode*)malloc(sizeof(GifLzwNode)*4096);

	memset(codetree, 0, sizeof(GifLzwNode)*4096);
	int curCode = -1;
	int codeSize = minCodeSize+1;
	int maxCode = clearCode+1;

	GifBitStatus stat;
	stat.byte = 0;
	stat.bitIndex = 0;
	stat.chunkIndex = 0;

	GifWriteCode(f, stat, clearCode, codeSize);  // start with a fresh LZW dictionary

	uchar nextValueSave;
	uchar tempValue;
	for(int yy=0; yy<height; ++yy)
	{
		for(int xx=0; xx<width; ++xx)
		{
			uchar nextValue = image[(yy*width+xx)*4+3];

			// "loser mode" - no compression, every single code is followed immediately by a clear
			//WriteCode( f, stat, nextValue, codeSize );
			//WriteCode( f, stat, 256, codeSize );

			int addCode=1;
			if( curCode < 0 )
			{
				// first value in a new run
				curCode = nextValue;
				addCode=0;
			}
			else if( codetree[curCode].m_next[nextValue] ||
				codetree[curCode].m_next[MAX(nextValue-7,0)] ||
				codetree[curCode].m_next[MAX(nextValue-6,0)] ||
				codetree[curCode].m_next[MAX(nextValue-5,0)] ||
				codetree[curCode].m_next[MAX(nextValue-4,0)] ||
				codetree[curCode].m_next[MAX(nextValue-3,0)] ||
				codetree[curCode].m_next[MAX(nextValue-2,0)] ||
				codetree[curCode].m_next[MAX(nextValue-1,0)] ||
				codetree[curCode].m_next[MIN(nextValue+1,255)] ||
				codetree[curCode].m_next[MIN(nextValue+2,255)] ||
				codetree[curCode].m_next[MIN(nextValue+3,255)] ||
				codetree[curCode].m_next[MIN(nextValue+4,255)] ||
				codetree[curCode].m_next[MIN(nextValue+5,255)] ||
				codetree[curCode].m_next[MIN(nextValue+6,255)] ||
				codetree[curCode].m_next[MIN(nextValue+7,255)])
			{
				// current run already in the dictionary
                nextValueSave=nextValue;

				tempValue=MIN(nextValueSave+7,255);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MAX(nextValueSave-7,0);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MIN(nextValueSave+6,255);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MAX(nextValueSave-6,0);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MIN(nextValueSave+5,255);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MAX(nextValueSave-5,0);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MIN(nextValueSave+4,255);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MAX(nextValueSave-4,0);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MIN(nextValueSave+3,255);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MAX(nextValueSave-3,0);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MIN(nextValueSave+2,255);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
					    nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MAX(nextValueSave-2,0);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}
				
				tempValue=MIN(nextValueSave+1,255);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=MAX(nextValueSave-1,0);
				if(codetree[curCode].m_next[tempValue])
				{
					if(abs(pPal->r[nextValue]-pPal->r[tempValue])<errorThres && abs(pPal->g[nextValue]-pPal->g[tempValue])<errorThres && abs(pPal->b[nextValue]-pPal->b[tempValue])<errorThres)
					{
						nextValue=tempValue;
						addCode=0;
					}
				}

				tempValue=nextValueSave;
				if(codetree[curCode].m_next[tempValue])
				{
					nextValue=tempValue;
					addCode=0;
				}

				if(addCode==0)
				    curCode = codetree[curCode].m_next[nextValue];
			}
			
			if(addCode==1)
			{
				// finish the current run, write a code
				GifWriteCode( f, stat, curCode, codeSize );

				// insert the new run into the dictionary
				codetree[curCode].m_next[nextValue] = ++maxCode;

				if( maxCode >= (1ul << codeSize) )
				{
					// dictionary entry count has broken a size barrier,
					// we need more bits for codes
					codeSize++;
				}
				if( maxCode == 4095 )
				{
					// the dictionary is full, clear it out and begin anew
					GifWriteCode(f, stat, clearCode, codeSize); // clear tree

					memset(codetree, 0, sizeof(GifLzwNode)*4096);
					curCode = -1;
					codeSize = minCodeSize+1;
					maxCode = clearCode+1;
				}

				curCode = nextValue;
			}
		}
	}

	// compression footer
	GifWriteCode( f, stat, curCode, codeSize );
	GifWriteCode( f, stat, clearCode, codeSize );
	GifWriteCode( f, stat, clearCode+1, minCodeSize+1 );

	// write out the last partial chunk
	while( stat.bitIndex ) GifWriteBit(stat, 0);
	if( stat.chunkIndex ) GifWriteChunk(f, stat);

	fputc(0, f); // image block terminator

	free(codetree);
}

struct GifWriter
{
	FILE* f;
	uchar* oldImage;
	bool firstFrame;
};

// Creates a gif file.
// The input GIFWriter is assumed to be uninitialized.
// The delay value is the time between frames in hundredths of a second - note that not all viewers pay much attention to this value.
bool GifBegin( GifWriter* writer, const char* filename, int width, int height, int delay, int bitDepth = 8, bool dither = false )
{
#if _MSC_VER >= 1400
	writer->f = 0;
	fopen_s(&writer->f, filename, "wb");
#else
	writer->f = fopen(filename, "wb");
#endif
	if(!writer->f) return false;

	writer->firstFrame = true;

	// allocate 
	writer->oldImage = (uchar*)malloc(width*height*4);

	fputs("GIF89a", writer->f);

	// screen descriptor
	fputc(width & 0xff, writer->f);
	fputc((width >> 8) & 0xff, writer->f);
	fputc(height & 0xff, writer->f);
	fputc((height >> 8) & 0xff, writer->f);

	fputc(0xf0, writer->f);  // there is an unsorted global color table of 2 entries
	fputc(0, writer->f);     // background color
	fputc(0, writer->f);     // pixels are square (we need to specify this because it's 1989)

	// now the "global" palette (really just a dummy palette)
	// color 0: black
	fputc(0, writer->f);
	fputc(0, writer->f); 
	fputc(0, writer->f);
	// color 1: also black
	fputc(0, writer->f);
	fputc(0, writer->f);
	fputc(0, writer->f);

	if( delay != 0 )
	{
		// animation header
		fputc(0x21, writer->f); // extension
		fputc(0xff, writer->f); // application specific
		fputc(11, writer->f); // length 11
		fputs("NETSCAPE2.0", writer->f); // yes, really
		fputc(3, writer->f); // 3 bytes of NETSCAPE2.0 data

		fputc(1, writer->f); // JUST BECAUSE
		fputc(0, writer->f); // loop infinitely (byte 0)
		fputc(0, writer->f); // loop infinitely (byte 1)

		fputc(0, writer->f); // block terminator
	}

	return true;
}

// Writes out a new frame to a GIF in progress.
// The GIFWriter should have been created by GIFBegin.
// AFAIK, it is legal to use different bit depths for different frames of an image -
// this may be handy to save bits in animations that don't change much.
bool GifWriteFrame( GifWriter* writer, const uchar* image, int width, int height, int delay, int bitDepth = 8, bool dither = false )
{
	if(!writer->f) return false;

	const uchar* oldImage = writer->firstFrame? NULL : writer->oldImage;
	writer->firstFrame = false;

	GifPalette pal;
	GifMakePalette((dither? NULL : oldImage), image, width, height, bitDepth, dither, &pal);

	if(dither)
		GifDitherImage(oldImage, image, writer->oldImage, width, height, &pal);
	else
		GifThresholdImage(oldImage, image, writer->oldImage, width, height, &pal);

	GifWriteLzwImage(writer->f, writer->oldImage, 0, 0, width, height, delay, &pal);

	return true;
}

// Writes the EOF code, closes the file handle, and frees temp memory used by a GIF.
// Many if not most viewers will still display a GIF properly if the EOF code is missing,
// but it's still a good idea to write it out.
bool GifEnd( GifWriter* writer )
{
	if(!writer->f) return false;

	fputc(0x3b, writer->f); // end of file
	fclose(writer->f);
	free(writer->oldImage);

	writer->f = NULL;
	writer->oldImage = NULL;

	return true;
}