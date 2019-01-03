package CrawlerAndQueryEngine;

import java.util.*;

/**
 * A web-index which efficiently stores information about pages. Serialization is done automatically
 * via the superclass "Index" and Java's Serializable interface.
 *
 */
public class WebIndex extends Index {
    /**
     * Needed for Serialization (provided by Index) - don't remove this!
     */
    private static final long serialVersionUID = 1L;

    private transient Page currentPage = null;

    private Map<Page, String[]> allPages = new HashMap<>();
    private Map<String, Map<Page, int[]>> dictionary = new HashMap<>();

    public void setCurrentPage(Page currentPage){
        this.currentPage = currentPage;
        currentPage.setID(allPages.size() + 1);
    }

    //Search the given word or phrase query for associated set of pages
    public Set<Page> search(String query) {
        if(query.charAt(0) != '\"'){
            //Use word search
            return searchWord(query);
        }
        //Use phrase search
        List<String> phrase = getPhrase(query);

        if(phrase.size() == 1){
            return searchWord(phrase.get(0));
        }
        return searchPhrase(phrase);
    }

    private List<String> getPhrase(String phrase) {
        if(phrase.charAt(0) == '\"') {
            phrase = phrase.substring(1, phrase.length() - 1);
        }
        List<String> text = new LinkedList<>();
        StringBuilder lastWord = new StringBuilder();
        for(int i = 0; i < phrase.length(); i++) {
            char character = phrase.charAt(i);
            if(Character.isLetterOrDigit(character)) {
                lastWord.append(character);
            } else {
                if(lastWord.length() > 0){
                    text.add(lastWord.toString());
                    lastWord = new StringBuilder();
                }
            }
        }
        if(lastWord.length() > 0) {
            text.add(lastWord.toString());
        }
        return text;
    }

    //Search the given phrase for associated set of pages
    private Set<Page> searchPhrase(Collection<String> phrase) {
        if(phrase.isEmpty()) {
            return new HashSet<>();
        }

        Iterator<String> phraseIt = phrase.iterator();

        if(phrase.size() == 1){
            return searchWord(phraseIt.next());
        }

        Map<Page, int[]> pageMap = dictionary.get(phraseIt.next());
        if(pageMap == null){
            return new HashSet<>();
        }

        Set<Page> pageSet = new HashSet<>(pageMap.keySet());

        //Find the intersection of the pages associated with each word in the phrase
        while(phraseIt.hasNext()){
            Map<Page, int[]> nextMap = dictionary.get(phraseIt.next());
            if(nextMap == null){
                return new HashSet<>();
            }
            pageSet.retainAll(nextMap.keySet());
            if(pageSet.size() == 0){
                return new HashSet<>();
            }
        }



        pageSet.removeIf((Page p) -> !hasPhrase(allPages.get(p), pageMap.get(p), phrase));

        return pageSet;
    }

    //Check whether a given set of nodes has at least one node representing the phrase
    private boolean hasPhrase(String[] wordList, int[] indexList, Collection<String> phrase) {
        main: for(int index: indexList) {
            if(index + phrase.size() > wordList.length) {
                return false;
            }

            index++;
            Iterator<String> it = phrase.iterator();
            it.next();

            while(it.hasNext()){
                String word = it.next();
                if(!wordList[index].equals(word)){
                    //This node doesn't match the given phrase
                    continue main;
                }
                index++;
            }
            return true;
        }
        //No nodes match the given phrase
        return false;
    }

    //Search the given word for associated set of pages
    public Set<Page> searchWord(String word) {
        if(word.charAt(0) == '!') {
            //return every page that doesn't contain this word
            return searchNotWord(word.substring(1));
        }
        if(!dictionary.containsKey(word)) {
            return new HashSet<>();
        }
        //return copySet(dictionary.get(word).keySet());
        return copySet(dictionary.get(word).keySet());
    }

    public Set<Page> searchNotWord(String word) {
        Set<Page> output = copySet(allPages.keySet());
        output.removeAll(searchWord(word));
        return output;
    }

    //Provides a copy of a given set
    //Useful because sets are passed by reference
    private Set<Page> copySet(Set<Page> pageSet) {
        return new HashSet<>(pageSet);
    }

    //Inserts the given phrase into dictionary
    public void addPhrase(Queue<String> phrase) {
        if(phrase.isEmpty()){
            return;
        }

        String[] phraseList = new String[phrase.size()];
        allPages.put(currentPage, phraseList);
        for(int i = 0; i < phraseList.length; i++){
            String word = getReference(phrase.remove());
            phraseList[i] = word;
            addToDictionary(word, i);
        }
    }

    private void addToDictionary(String word, int index) {
        if(!dictionary.containsKey(word)) {
            Map<Page, int[]> pageMap = new HashMap<>();
            dictionary.put(word, pageMap);
            int[] indexList = new int[1];
            pageMap.put(currentPage, indexList);
            indexList[0] = index;
            return;
        }
        Map<Page, int[]> pageMap = dictionary.get(word);
        if(pageMap.containsKey(currentPage)) {
            int[] indexList = new int[pageMap.get(currentPage).length + 1];
            for(int i = 0; i < pageMap.get(currentPage).length; i++) {
                indexList[i] = pageMap.get(currentPage)[i];
            }
            indexList[pageMap.get(currentPage).length] = index;
            pageMap.put(currentPage, indexList);
            return;
        }
        int[] indexList = new int[1];
        pageMap.put(currentPage, indexList);
        indexList[0] = index;
    }

    //Returns a pointer to a given String if it already exists in the structure
    //Useful for saving space
    private String getReference(String word){
        if(dictionary.containsKey(word)){
            Iterator<Page> pageIt = dictionary.get(word).keySet().iterator();
            Page current = pageIt.next();
            return allPages.get(current)[dictionary.get(word).get(current)[0]];
        }
        return word;
    }



    public Set<Page> inverse(Set<Page> input) {
        Set<Page> output = copySet(allPages.keySet());
        output.removeAll(input);
        return output;
    }


    public Set<Page> searchPhraseAdd(String phrase, Set<Page> intersection) {
        return searchPhraseAdd(getPhrase(phrase), intersection);
    }

    private Set<Page> searchPhraseAdd(Collection<String> phrase, Set<Page> intersection) {
        if(phrase.size() == 0) {
            return new HashSet<>();
        }

        Iterator<String> phraseIt = phrase.iterator();

        if(phrase.size() == 1){
            Set<Page> temp = searchWord(phraseIt.next());
            temp.retainAll(intersection);
            return temp;
        }

        Map<Page, int[]> pageMap = dictionary.get(phraseIt.next());
        if(pageMap == null){
            return new HashSet<>();
        }

        intersection.retainAll(pageMap.keySet());
        if(intersection.isEmpty()){
            return new HashSet<>();
        }

        while(phraseIt.hasNext()){
            Map<Page, int[]> nextMap = dictionary.get(phraseIt.next());
            if(nextMap == null){
                return new HashSet<>();
            }
            intersection.retainAll(nextMap.keySet());
            if(intersection.isEmpty()){
                return new HashSet<>();
            }
        }

        intersection.removeIf((Page p) -> !hasPhrase(allPages.get(p), pageMap.get(p), phrase));

        return intersection;
    }

    public Set<Page> searchPhraseRemove(String phrase, Set<Page> remove) {
        return searchPhraseRemove(getPhrase(phrase), remove);
    }

    public Set<Page> searchPhraseRemove(Collection<String> phrase, Set<Page> remove) {
        if(phrase.isEmpty()) {
            return new HashSet<>();
        }

        Iterator<String> phraseIt = phrase.iterator();

        if(phrase.size() == 1){
            Set<Page> temp = searchWord(phraseIt.next());
            temp.removeAll(remove);
            return temp;
        }

        Map<Page, int[]> pageMap = dictionary.get(phraseIt.next());
        if(pageMap == null){
            return new HashSet<>();
        }

        Set<Page> pageSet = new HashSet<>(pageMap.keySet());

        pageSet.removeAll(remove);
        if(pageSet.isEmpty()){
            return new HashSet<>();
        }

        while(phraseIt.hasNext()){
            Map<Page, int[]> nextMap = dictionary.get(phraseIt.next());
            if(nextMap == null){
                return new HashSet<>();
            }
            pageSet.retainAll(nextMap.keySet());
            if(pageSet.isEmpty()){
                return new HashSet<>();
            }
        }

        pageSet.removeIf((Page p) -> !hasPhrase(allPages.get(p), pageMap.get(p), phrase));

        return pageSet;
    }
}
