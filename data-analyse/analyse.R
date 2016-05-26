library(dplyr)
library(ggplot2)
library(car)

alfa = 0.05

results = read.csv("masresults.csv", sep = ";", header = TRUE)

MASplot = function(xAxis, yAxis, filterQuery = "", title = "", xlabel = xAxis, ylabel = yAxis) {
  if(filterQuery == ""){
    if(title == ""){
      title = "Using all results"
    }
    data = results
  } else {
    if(title == ""){
      title = filterQuery
    }else{
      title = paste(title, "(", filterQuery,")")
    }
    data = results %>% filter_(filterQuery)
  }

  data = data %>%
    group_by_("protocolType", xAxis) %>%
    summarise_(aggregatedY = paste("mean(",yAxis,")"), x=paste("first(",xAxis,")"), se = paste("sd(",yAxis,")/sqrt(n())"), n = "n()")

  ggplot(data, aes(x=x, y=aggregatedY, col = protocolType)) +
    geom_line() +
    geom_point() +
    geom_errorbar(aes(ymin = aggregatedY - se*qt(1-alfa/2, df=n), ymax = aggregatedY + se*qt(1-alfa/2, df=n)), width = .3) +
    xlab(xlabel) +
    ylab(ylabel) +
    ggtitle(title)
}
MASplot("nbDrones", "totalProfit")
MASplot("nbWarehouses", "totalProfit")
MASplot("nbInitialClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")

MASplot("nbWarehouses", "totalProfit", "nbDrones >= 7")
MASplot("nbInitialClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")

MASplot("nbDrones", "averageDeliveryTime")
MASplot("nbWarehouses", "averageDeliveryTime")
MASplot("nbInitialClients", "averageDeliveryTime")
MASplot("nbDynamicClients", "averageDeliveryTime")

MASplot("nbDrones", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbWarehouses", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbInitialClients", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbDynamicClients", "averageDeliveryTime", "nbDrones >= 7")

MASplot("nbClients", "totalProfit", "nbDrones >= 8")




s = results %>% filter(nbDrones == 8 & nbWarehouses == 10)
# Test for homoskedasticiteit: mag niet significant zijn, Pr(>F) moet >0.05 zijn, of er mogen dus geen sterretje(s) naast resultaat staan
# Anders werkt TukeyHSD NIET!
leveneTest(totalProfit ~ protocolType, s)
s.aov = aov(totalProfit ~ protocolType, s)
summary(s.aov)
TukeyHSD(s.aov)

s = results %>% filter(nbDrones == 3 & nbDynamicClients == 40 & nbInitialClients == 10 & nbWarehouses == 8)
boxplot(totalProfit ~ protocolType, s)
leveneTest(totalProfit ~ protocolType, s)
s.aov = aov(totalProfit ~ protocolType, s)
summary(s.aov)
TukeyHSD(s.aov)
