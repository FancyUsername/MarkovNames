package de.sommer.test;

public class SearchResult {
        private final String id;
        private final String title;
        private final String price;
        private final String location;
        private final String url;

        public SearchResult(String id, String title, String price, String location, String url) {
                this.id = id;
                this.title = title;
                this.price = price;
                this.location = location;
                this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getPrice() {
            return price;
        }

        public String getLocation() {
            return location;
        }

        public String getUrl() {
            return url;
        }
}
