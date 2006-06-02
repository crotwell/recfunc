#! /usr/bin/python

import csv, os, time, glob

def readData(dataFile, gmt, columns=[2,1,4], infinityVal=-1, minLat=-90, maxLat=90, minLon=-180, maxLon=180, minEQ=0):
    print 'reading '+dataFile
    results = csv.reader(open(dataFile, 'r'))
    mean=[0,0,0,0,0,0,0,0,0,0,0,0,0]
    num=[0,0,0,0,0,0,0,0,0,0,0,0,0]
    for row in results:
    	if row[0].startswith('#'):
	    continue
	lat = float(row[1])
	lon = float(row[2])
	if (lat < minLat or lat > maxLat or lon < minLon or lon > maxLon):
	    continue
	if (int(row[10]) < minEQ):
	    continue
	line = ''
	for col in columns:
	    if row[col] == 'Infinity':
		row[col] = '-1'
	    else:
		mean[col] = mean[col] + float(row[col])
		num[col] = num[col]+1
		gmt.write(row[col]+' ')
		line = line + row[col]+' '
	gmt.write('\n')
	print(line)
	mean[col] = mean[col]/num[col]
	print 'mean[%i]=%f num=%f' % (col, mean[col], num[col])

def makeSmallCircleXY(lat, lon, dist):
    gmt, out = os.popen2('project -T%f/%f -C0/-90 -Fpq '% (lon, lat), 'w')
    gmt.write('0 90\n')
    gmt.write('0 0\n')
    gmt.close()
    tlonlat = out.readline().split()
    clonlat = out.readline().split()
    print tlonlat
    print clonlat
    gmt, out = os.popen2('project -T%s/%s -C%s/%s -Fpq' % (tlonlat[0], tlonlat[1], clonlat[0], clonlat[1]), 'w')
    for i in range(0, 360, 1):
	gmt.write('%f %f\n' % (i, (90-dist)))
    gmt.close()
    lines = out.readlines()
    for line in lines:
#	print line
	pass
    return lines

def makeMap(dataFile, outFile):
    out = ' >> '+outFile
    xyRegion = '-126/-110/30/50'
    proj = '-JM5i'
    cptFile = 'rate.cpt'
    gmt= os.popen('pscoast -P -K '+proj+' -R' +xyRegion+' -Dh -Na -W '+out, 'w')
    gmt.close()

    gmt= os.popen('psxy -O -K -B2WESn -P '+proj+' -R'+xyRegion+' -C'+cptFile+' -St.2i -G0  '+out, 'w')
    readData(dataFile, gmt, [2, 1, 4])
    gmt.close()

    gmt = os.popen('psscale -D6i/7i/5i/.5i -C'+cptFile+' -B5 -P -O  '+out, 'w')
    gmt.close()

def makeHisto(dataFile, outFile, col=4, width=10):
    out = ' >> '+outFile
    gmt = os.popen('pshistogram -JX6i -W'+str(width)+' -G120 '+out, 'w')
    readData(dataFile, gmt, [col])
    gmt.close()

def makeXY(outFile, xcol=1, ycol=6):
    symbols = 'a c d h i n s t x'.split()
    out = ' >> '+outFile
    xyRegion = '30/50/-0.05/.6'
    proj = '-JX6i'
    tickInfo=' -B2:Latitude:/.1:Winner/Total: '
    cptFile = 'rate.cpt'
    gmt= os.popen('psbasemap -P -K '+proj+' -R'+xyRegion+tickInfo+out, 'w')
    gmt.close()

    num = 0
    color='-G0'
    for f in glob.glob('data/*.csv'):
	symbol = symbols[num]
	if (f=='data/TAresults.csv'):
	    color='-G255/0/0'
	elif (f=='data/BKresults.csv'):
	    color='-G255/255/0'
	elif (f=='data/CIresults.csv'):
	    color='-G0/255/0'
	elif (f=='data/TSresults.csv'):
	    color='-G0/120/255'
	elif (f=='data/AZresults.csv'):
	    color='-G0/255/255'
	else:
	    color=''
	    continue
	gmt= os.popen('psxy -O -K -P '+proj+' -R'+xyRegion+'  -S'+symbol+'.1i '+color+'  '+out, 'w')
	readData(f, gmt, [xcol, ycol], minLon=-126, maxLon=-114, minEQ=5)
	gmt.close()
	num+=1
	if (num >= len(symbols)):
	    num = 0

    gmt= os.popen('psbasemap -P -O '+proj+' -R'+xyRegion+tickInfo+out, 'w')
    gmt.close()
    
def distCircleMap(outFile):
    out = ' >> '+outFile
    xyRegion = '-180/180/-80/80'
    proj = '-JE-120/40/6i'
    gmt=os.popen('pscoast -P -K '+proj+' -R' +xyRegion+' -Dc  -W '+out, 'w')
    gmt.close()

    for lat in [30, 40, 50]:
	lines=makeSmallCircleXY(lat, -120, 90)
	gmt= os.popen('psxy -O -K -P '+proj+' -R'+xyRegion+'  -W   '+out, 'w')
	for line in lines:
	    gmt.write(line)
	gmt.close()
    
    gmt=os.popen('pscoast -P -O '+proj+' -R' +xyRegion+' -Dc  -W '+out, 'w')
    gmt.close()
	    
def ps2pdf(outFilename):
    ps2pdf = os.popen('pstopdf '+outFilename)
    ps2pdf.close()
    try:
	os.remove(outFilename)
	pass
    except OSError:
	pass


#gmtset = os.popen('gmtset OUTPUT_DEGREE_FORMAT=-ddd PLOT_DEGREE_FORMAT=-ddd')
#gmtset.close()

datafile = 'data/TAresults.csv'

#outFilename = 'mapTA.ps'
#makeMap(datafile, outFilename)
#ps2pdf(outFilename)


#outFilename = 'histoTA_sPeriod.ps'
#makeHisto(datafile, outFilename, 4, 5)
#ps2pdf(outFilename)

#outFilename = 'histoTA_tPeriod.ps'
#makeHisto(datafile, outFilename, 5, 5)
#ps2pdf(outFilename)

#outFilename = 'histoTA_ratio.ps'
#makeHisto(datafile, outFilename, 6, .02)
#ps2pdf(outFilename)

#outFilename = 'lat_ratio.ps'
#makeXY(outFilename)
#ps2pdf(outFilename)

outFilename = 'latCircles.ps'
distCircleMap(outFilename)
ps2pdf(outFilename)
