from pandas import read_csv
from matplotlib import pyplot

series = read_csv('./inputs/daily-total-female-births.csv', header=0, index_col=0)
pyplot.plot(series)
pyplot.show()
pyplot.savefig('./outputs/female-daily-births.png')