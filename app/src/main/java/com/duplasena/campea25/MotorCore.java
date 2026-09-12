package com.duplasena.campea25;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public final class MotorCore {
    private MotorCore() {}

    public interface ProgressCallback { void onProgress(int percent, String message); }

    public static final class Contest {
        public final int contest;
        public final int[] draw1;
        public final int[] draw2;
        public Contest(int contest, int[] draw1, int[] draw2) {
            this.contest = contest;
            this.draw1 = draw1;
            this.draw2 = draw2;
        }
    }

    public static final class GroupStats {
        public final int[] group;
        public final int hits1, hits2, hitsTotal;
        public final int delayEvents, delayContests, lastHitContest;
        public final double pressure;
        public GroupStats(int[] group, int hits1, int hits2, int delayEvents, int delayContests, int lastHitContest, double pressure) {
            this.group = group; this.hits1 = hits1; this.hits2 = hits2; this.hitsTotal = hits1 + hits2;
            this.delayEvents = delayEvents; this.delayContests = delayContests; this.lastHitContest = lastHitContest; this.pressure = pressure;
        }
    }

    public static final class FilterConfig {
        public int evenMin, evenMax, primeMin, primeMax, fibMin, fibMax, lowMin, lowMax, sumMin, sumMax, seqMax;
        public int repeatUnionMode;
        @Override public String toString() {
            return "pares="+evenMin+"-"+evenMax+" | primos="+primeMin+"-"+primeMax+" | fib="+fibMin+"-"+fibMax+
                    " | 01-25="+lowMin+"-"+lowMax+" | soma="+sumMin+".."+sumMax+" | seq<="+seqMax+" | rep união≈"+repeatUnionMode;
        }
    }

    public static final class GameMetrics {
        public final int[] game;
        public final double score;
        public final int q2_1,q3_1,q4_1,q5_1;
        public final int q2_2,q3_2,q4_2,q5_2;
        public final int repeatLast1, repeatLast2, repeatLastUnion;
        public final int even, prime, fib, low, sum, maxSeq;
        public final double numberScore;
        public GameMetrics(int[] game,double score,int q2_1,int q3_1,int q4_1,int q5_1,int q2_2,int q3_2,int q4_2,int q5_2,
                           int repeatLast1,int repeatLast2,int repeatLastUnion,int even,int prime,int fib,int low,int sum,int maxSeq,double numberScore) {
            this.game=game; this.score=score; this.q2_1=q2_1; this.q3_1=q3_1; this.q4_1=q4_1; this.q5_1=q5_1;
            this.q2_2=q2_2; this.q3_2=q3_2; this.q4_2=q4_2; this.q5_2=q5_2;
            this.repeatLast1=repeatLast1; this.repeatLast2=repeatLast2; this.repeatLastUnion=repeatLastUnion;
            this.even=even; this.prime=prime; this.fib=fib; this.low=low; this.sum=sum; this.maxSeq=maxSeq; this.numberScore=numberScore;
        }
    }

    public static final class GameResult {
        public final GroupStats groupStats;
        public final FilterConfig filter;
        public final GameMetrics best;
        public final int totalCombinations, classified;
        public final int perimeterContests;
        public GameResult(GroupStats groupStats, FilterConfig filter, GameMetrics best, int totalCombinations, int classified, int perimeterContests) {
            this.groupStats=groupStats; this.filter=filter; this.best=best; this.totalCombinations=totalCombinations; this.classified=classified; this.perimeterContests=perimeterContests;
        }
    }

    private static final Set<Integer> PRIMES = new HashSet<>(Arrays.asList(2,3,5,7,11,13,17,19,23,29,31,37,41,43,47));
    private static final Set<Integer> FIB = new HashSet<>(Arrays.asList(1,2,3,5,8,13,21,34));
    private static final int PRESELECT = 1800;

    public static List<Contest> parseHistory(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.indexOf('\uFFFD') >= 0) text = new String(bytes, java.nio.charset.Charset.forName("ISO-8859-1"));
        List<Contest> out = new ArrayList<>();
        int fallback = 1;
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            List<Integer> vals = new ArrayList<>();
            Matcher m = Pattern.compile("\\d+").matcher(line);
            while (m.find()) {
                try { vals.add(Integer.parseInt(m.group())); } catch (Exception ignored) {}
            }
            if (vals.size() < 12) continue;
            int start = findDrawWindow(vals);
            if (start < 0) continue;
            int[] d1 = new int[6], d2 = new int[6];
            for (int i=0;i<6;i++) d1[i]=vals.get(start+i);
            for (int i=0;i<6;i++) d2[i]=vals.get(start+6+i);
            Arrays.sort(d1); Arrays.sort(d2);
            int contest = extractContest(vals, start, fallback);
            out.add(new Contest(contest,d1,d2)); fallback++;
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Não identifiquei linhas com 2 sorteios de 6 dezenas (01 a 50).");
        // Deduplicate contest numbers: keep last parsed line for same contest.
        TreeMap<Integer,Contest> map = new TreeMap<>();
        for (Contest c: out) map.put(c.contest,c);
        return new ArrayList<>(map.values());
    }

    private static int findDrawWindow(List<Integer> vals) {
        int best=-1;
        for (int s=0;s+11<vals.size();s++) {
            boolean ok=true;
            HashSet<Integer> a=new HashSet<>(), b=new HashSet<>();
            for(int i=0;i<6;i++){int v=vals.get(s+i); if(v<1||v>50||!a.add(v)){ok=false;break;}}
            if(!ok)continue;
            for(int i=0;i<6;i++){int v=vals.get(s+6+i); if(v<1||v>50||!b.add(v)){ok=false;break;}}
            if(ok) best=s; // prefer last valid window, usually after date/metadata
        }
        return best;
    }

    private static int extractContest(List<Integer> vals,int drawStart,int fallback){
        for(int i=0;i<drawStart;i++){int v=vals.get(i); if(v>50 && v<100000) return v;}
        if(drawStart>0){int v=vals.get(0); if(v>0) return v;}
        return fallback;
    }

    public static String fmt(int[] xs){StringBuilder s=new StringBuilder();for(int i=0;i<xs.length;i++){if(i>0)s.append(' ');s.append(String.format(Locale.US,"%02d",xs[i]));}return s.toString();}

    private static long mask(int[] xs){long m=0;for(int x:xs)m|=1L<<(x-1);return m;}
    private static int[] arr(long m,int want){int[] a=new int[want];int k=0;for(int n=1;n<=50&&k<want;n++)if((m&(1L<<(n-1)))!=0)a[k++]=n;return a;}
    private static int pop(long m){return Long.bitCount(m);}

    public static GroupStats evaluateGroup(List<Contest> hist,int[] group){return evaluateGroupMask(hist,mask(group));}

    private static GroupStats evaluateGroupMask(List<Contest> hist,long gm){
        int h1=0,h2=0,lastEvent=-1,event=-1,lastContest=-1;
        for(Contest c:hist){
            event++; long a=mask(c.draw1); if((a&gm)==a){h1++;lastEvent=event;lastContest=c.contest;}
            event++; long b=mask(c.draw2); if((b&gm)==b){h2++;lastEvent=event;lastContest=c.contest;}
        }
        int delayEvents = lastEvent<0 ? hist.size()*2 : (hist.size()*2-1-lastEvent);
        int maxContest=hist.get(hist.size()-1).contest;
        int delayContests=lastContest<0?hist.size():Math.max(0,maxContest-lastContest);
        int hits=h1+h2;
        double meanGap = hits==0 ? hist.size()*2.0 : (hist.size()*2.0/hits);
        double pressure = meanGap<=0?0:delayEvents/meanGap;
        return new GroupStats(arr(gm,25),h1,h2,delayEvents,delayContests,lastContest,pressure);
    }

    public static GroupStats searchChampion25(List<Contest> hist,int iterations,ProgressCallback cb){
        if(hist.size()<40)throw new IllegalArgumentException("Histórico pequeno demais para pesquisar grupo campeão.");
        double[] strength=numberStrength(hist);
        long best=topMask(strength,25); GroupStats bestS=evaluateGroupMask(hist,best);
        long current=best; GroupStats currentS=bestS;
        Random rnd=new Random(0xD05EEDL + hist.get(hist.size()-1).contest*97L);
        iterations=Math.max(1200,iterations);
        for(int it=0;it<iterations;it++){
            long cand;
            if(it%17==0) cand=randomWeightedMask(strength,rnd,25);
            else cand=mutateMask(current,rnd,1+(it%3));
            GroupStats s=evaluateGroupMask(hist,cand);
            if(betterChampion(s,bestS)){best=cand;bestS=s;}
            // local walk: accept improvement or occasional near-tie to escape plateaus
            if(betterChampion(s,currentS) || (it%29==0 && s.hitsTotal>=currentS.hitsTotal-1)){current=cand;currentS=s;}
            if((it+1)%Math.max(1,iterations/20)==0 && cb!=null)cb.onProgress(5+(int)(30.0*(it+1)/iterations),"Buscando grupo campeão 25 • "+(it+1)+"/"+iterations+" • 6/6="+bestS.hitsTotal);
        }
        return bestS;
    }

    private static boolean betterChampion(GroupStats a,GroupStats b){
        if(a.hitsTotal!=b.hitsTotal)return a.hitsTotal>b.hitsTotal;
        int amin=Math.min(a.hits1,a.hits2), bmin=Math.min(b.hits1,b.hits2); if(amin!=bmin)return amin>bmin;
        if(a.delayEvents!=b.delayEvents)return a.delayEvents<b.delayEvents;
        return lexLess(a.group,b.group);
    }

    public static GroupStats searchDelayedStrong25(List<Contest> hist,GroupStats champion,int iterations,ProgressCallback cb){
        double[] strength=numberStrength(hist); long champ=mask(champion.group); long best=0; GroupStats bestS=null;
        int floor=Math.max(8,(int)Math.floor(champion.hitsTotal*0.55));
        Random rnd=new Random(0xA771AD0L + hist.get(hist.size()-1).contest*131L);
        long current=champ;
        iterations=Math.max(1800,iterations);
        for(int it=0;it<iterations;it++){
            long cand;
            if(it%23==0)cand=randomWeightedMask(strength,rnd,25); else cand=mutateMask(current,rnd,2+(it%4));
            if(cand==champ)continue;
            GroupStats s=evaluateGroupMask(hist,cand);
            if(s.hitsTotal>=floor && (bestS==null || betterDelayed(s,bestS))){best=cand;bestS=s;current=cand;}
            else if(s.hitsTotal>=floor && it%31==0) current=cand;
            if((it+1)%Math.max(1,iterations/20)==0 && cb!=null)cb.onProgress(35+(int)(20.0*(it+1)/iterations),"Buscando grupo forte mais atrasado • mínimo histórico "+floor+" • "+(bestS==null?"aguardando":("falhou "+bestS.delayEvents+" sorteios")));
        }
        if(bestS==null){
            // guaranteed fallback: mutate champion one swap and pick strongest delay found.
            for(int i=0;i<800;i++){long cand=mutateMask(champ,rnd,1);GroupStats s=evaluateGroupMask(hist,cand);if(cand!=champ&&(bestS==null||betterDelayed(s,bestS)))bestS=s;}
        }
        return bestS;
    }

    private static boolean betterDelayed(GroupStats a,GroupStats b){
        if(a.delayEvents!=b.delayEvents)return a.delayEvents>b.delayEvents;
        if(a.delayContests!=b.delayContests)return a.delayContests>b.delayContests;
        if(a.hitsTotal!=b.hitsTotal)return a.hitsTotal>b.hitsTotal;
        if(a.pressure!=b.pressure)return a.pressure>b.pressure;
        return lexLess(a.group,b.group);
    }

    private static boolean lexLess(int[] a,int[] b){for(int i=0;i<Math.min(a.length,b.length);i++){if(a[i]!=b[i])return a[i]<b[i];}return a.length<b.length;}

    private static long topMask(double[] strength,int k){Integer[] n=new Integer[50];for(int i=0;i<50;i++)n[i]=i+1;Arrays.sort(n,(a,b)->Double.compare(strength[b],strength[a]));long m=0;for(int i=0;i<k;i++)m|=1L<<(n[i]-1);return m;}
    private static long randomWeightedMask(double[] strength,Random rnd,int k){
        double min=Double.POSITIVE_INFINITY;for(int n=1;n<=50;n++)min=Math.min(min,strength[n]);
        long m=0;while(pop(m)<k){double total=0;for(int n=1;n<=50;n++)if((m&(1L<<(n-1)))==0)total+=1+Math.max(0,strength[n]-min);double r=rnd.nextDouble()*total;for(int n=1;n<=50;n++)if((m&(1L<<(n-1)))==0){r-=1+Math.max(0,strength[n]-min);if(r<=0){m|=1L<<(n-1);break;}}}return m;
    }
    private static long mutateMask(long m,Random rnd,int swaps){
        for(int s=0;s<swaps;s++){
            int out;do{out=1+rnd.nextInt(50);}while((m&(1L<<(out-1)))==0);
            int in;do{in=1+rnd.nextInt(50);}while((m&(1L<<(in-1)))!=0);
            m&=~(1L<<(out-1));m|=1L<<(in-1);
        }return m;
    }

    private static double[] numberStrength(List<Contest> hist){
        int events=hist.size()*2; int[][] seen=new int[51][events]; int e=0;
        for(Contest c:hist){for(int x:c.draw1)seen[x][e]=1;e++;for(int x:c.draw2)seen[x][e]=1;e++;}
        double[] out=new double[51];
        for(int n=1;n<=50;n++){
            int total=0,f20=0,f50=0,f100=0,delay=events;double slope=0;for(int i=0;i<events;i++){if(seen[n][i]==1){total++;delay=events-1-i;}if(i>=events-20)f20+=seen[n][i];if(i>=events-50)f50+=seen[n][i];if(i>=events-100)f100+=seen[n][i];}
            int st=Math.max(0,events-30), len=events-st; if(len>1){double xm=(len-1)/2.0,ym=0;for(int i=st;i<events;i++)ym+=seen[n][i];ym/=len;double den=0,num=0;for(int j=0;j<len;j++){double dx=j-xm;den+=dx*dx;num+=dx*(seen[n][st+j]-ym);}if(den>0)slope=num/den;}
            double delayBonus=Math.max(0,8-Math.abs(delay-4));
            out[n]=total*.08+f100*.28+f50*.6+f20*1.4+Math.max(0,slope)*70+delayBonus;
        }
        return out;
    }

    public static FilterConfig learnFilter(List<Contest> hist){
        ArrayList<Integer> ev=new ArrayList<>(), pr=new ArrayList<>(), fi=new ArrayList<>(), lo=new ArrayList<>(), su=new ArrayList<>(), sq=new ArrayList<>(), reps=new ArrayList<>();
        long prevUnion=0;
        for(int i=0;i<hist.size();i++){
            Contest c=hist.get(i); long unionPrev=i>0?(mask(hist.get(i-1).draw1)|mask(hist.get(i-1).draw2)):0;
            int[][] ds={c.draw1,c.draw2};
            for(int[] g:ds){ev.add(evens(g));pr.add(countSet(g,PRIMES));fi.add(countSet(g,FIB));lo.add(low(g));su.add(sum(g));sq.add(maxSeq(g));if(i>0)reps.add(pop(mask(g)&unionPrev));}
        }
        FilterConfig f=new FilterConfig();
        f.evenMin=pct(ev,.05);f.evenMax=pct(ev,.95);f.primeMin=pct(pr,.05);f.primeMax=pct(pr,.95);f.fibMin=pct(fi,.05);f.fibMax=pct(fi,.95);f.lowMin=pct(lo,.05);f.lowMax=pct(lo,.95);f.sumMin=pct(su,.05);f.sumMax=pct(su,.95);f.seqMax=Math.max(2,pct(sq,.95));f.repeatUnionMode=mode(reps);
        return f;
    }

    private static int pct(List<Integer> a,double p){if(a.isEmpty())return 0;ArrayList<Integer>b=new ArrayList<>(a);Collections.sort(b);int idx=(int)Math.round((b.size()-1)*p);return b.get(Math.max(0,Math.min(b.size()-1,idx)));}
    private static int mode(List<Integer>a){if(a.isEmpty())return 1;HashMap<Integer,Integer>c=new HashMap<>();for(int x:a)c.put(x,c.getOrDefault(x,0)+1);int bk=0,bv=-1;for(Map.Entry<Integer,Integer>e:c.entrySet())if(e.getValue()>bv||(e.getValue()==bv&&e.getKey()<bk)){bk=e.getKey();bv=e.getValue();}return bk;}

    private static final class Candidate implements Comparable<Candidate>{final int[] g;final double base;final int even,prime,fib,low,sum,seq,rep1,rep2,repU;Candidate(int[]g,double base,int even,int prime,int fib,int low,int sum,int seq,int rep1,int rep2,int repU){this.g=g;this.base=base;this.even=even;this.prime=prime;this.fib=fib;this.low=low;this.sum=sum;this.seq=seq;this.rep1=rep1;this.rep2=rep2;this.repU=repU;}public int compareTo(Candidate o){return Double.compare(base,o.base);}}

    public static GameResult generateGame(List<Contest> hist,GroupStats gs,ProgressCallback cb){
        FilterConfig f=learnFilter(hist); double[] ns=numberStrength(hist); int[] pool=gs.group.clone();Arrays.sort(pool);
        Contest last=hist.get(hist.size()-1);long last1=mask(last.draw1),last2=mask(last.draw2),lastU=last1|last2;
        PriorityQueue<Candidate> heap=new PriorityQueue<>();int total=0,classified=0;final int COMB=177100;
        int n=pool.length;
        for(int a=0;a<n-5;a++)for(int b=a+1;b<n-4;b++)for(int c=b+1;c<n-3;c++)for(int d=c+1;d<n-2;d++)for(int e=d+1;e<n-1;e++)for(int z=e+1;z<n;z++){
            int[] g={pool[a],pool[b],pool[c],pool[d],pool[e],pool[z]};total++;
            int even=evens(g),prime=countSet(g,PRIMES),fib=countSet(g,FIB),low=low(g),sum=sum(g),seq=maxSeq(g);
            if(even<f.evenMin||even>f.evenMax||prime<f.primeMin||prime>f.primeMax||fib<f.fibMin||fib>f.fibMax||low<f.lowMin||low>f.lowMax||sum<f.sumMin||sum>f.sumMax||seq>f.seqMax)continue;
            classified++;long gm=mask(g);int r1=pop(gm&last1),r2=pop(gm&last2),ru=pop(gm&lastU);double base=0;for(int x:g)base+=ns[x];base-=Math.abs(ru-f.repeatUnionMode)*4.0;base+=Math.min(r1,r2)*1.5;
            Candidate cc=new Candidate(g,base,even,prime,fib,low,sum,seq,r1,r2,ru);if(heap.size()<PRESELECT)heap.add(cc);else if(cc.base>heap.peek().base){heap.poll();heap.add(cc);}
            if(total%8000==0&&cb!=null)cb.onProgress(55+(int)(20.0*total/COMB),"Percorrendo 177.100 jogos • "+total+" • classificados "+classified);
        }
        if(heap.isEmpty())throw new IllegalStateException("Nenhuma combinação passou pelo filtro aprendido.");
        ArrayList<Candidate> top=new ArrayList<>(heap);top.sort((x,y)->Double.compare(y.base,x.base));
        int perimeter=Math.min(160,hist.size());int start=hist.size()-perimeter;GameMetrics best=null;int idx=0;
        for(Candidate cc:top){idx++;long gm=mask(cc.g);int q21=0,q31=0,q41=0,q51=0,q22=0,q32=0,q42=0,q52=0;boolean hot6=false;double recent=0;
            for(int i=start;i<hist.size();i++){
                Contest ct=hist.get(i);int h1=pop(gm&mask(ct.draw1)),h2=pop(gm&mask(ct.draw2));int age=hist.size()-1-i;double w=age<10?3.0:age<30?1.8:age<60?1.2:1.0;
                if(h1==6||h2==6){hot6=true;break;}
                if(h1==2)q21++;else if(h1==3)q31++;else if(h1==4)q41++;else if(h1==5)q51++;
                if(h2==2)q22++;else if(h2==3)q32++;else if(h2==4)q42++;else if(h2==5)q52++;
                recent+=w*(hitWeight(h1)+hitWeight(h2));
            }
            if(hot6)continue;
            double balance= -Math.abs((q41+q51)-(q42+q52))*2.0;
            double score=cc.base*0.35+(q51+q52)*50+(q41+q42)*19+(q31+q32)*5.5+(q21+q22)*.7+recent*.18+balance;
            GameMetrics gmtr=new GameMetrics(cc.g.clone(),score,q21,q31,q41,q51,q22,q32,q42,q52,cc.rep1,cc.rep2,cc.repU,cc.even,cc.prime,cc.fib,cc.low,cc.sum,cc.seq,cc.base);
            if(best==null||betterGame(gmtr,best))best=gmtr;
            if(idx%150==0&&cb!=null)cb.onProgress(75+(int)(24.0*idx/top.size()),"Perímetro duplo • candidato "+idx+"/"+top.size()+" • melhor "+(best==null?"-":fmt(best.game)));
        }
        if(best==null)throw new IllegalStateException("Todos os candidatos fortes já fizeram sena no perímetro. Ajuste a base ou o filtro.");
        if(cb!=null)cb.onProgress(100,"Jogo concluído.");
        return new GameResult(gs,f,best,total,classified,perimeter);
    }

    private static boolean betterGame(GameMetrics a,GameMetrics b){
        int a5=a.q5_1+a.q5_2,b5=b.q5_1+b.q5_2;if(a5!=b5)return a5>b5;
        int a4=a.q4_1+a.q4_2,b4=b.q4_1+b.q4_2;if(a4!=b4)return a4>b4;
        int a3=a.q3_1+a.q3_2,b3=b.q3_1+b.q3_2;if(a3!=b3)return a3>b3;
        if(a.score!=b.score)return a.score>b.score;
        return lexLess(a.game,b.game);
    }
    private static double hitWeight(int h){switch(h){case 5:return 16;case 4:return 7;case 3:return 2.5;case 2:return .4;default:return 0;}}

    private static int evens(int[]g){int c=0;for(int x:g)if(x%2==0)c++;return c;}
    private static int countSet(int[]g,Set<Integer>s){int c=0;for(int x:g)if(s.contains(x))c++;return c;}
    private static int low(int[]g){int c=0;for(int x:g)if(x<=25)c++;return c;}
    private static int sum(int[]g){int s=0;for(int x:g)s+=x;return s;}
    private static int maxSeq(int[]g){int[]a=g.clone();Arrays.sort(a);int best=1,cur=1;for(int i=1;i<a.length;i++){if(a[i]==a[i-1]+1){cur++;best=Math.max(best,cur);}else cur=1;}return best;}
}
