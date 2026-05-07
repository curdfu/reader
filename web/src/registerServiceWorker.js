/* eslint-disable no-console */

import { register } from "register-service-worker";

function isLocalServeHost() {
  const hostname = window.location.hostname;
  return (
    hostname === "localhost" || hostname === "127.0.0.1" || hostname === "::1"
  );
}

function unregisterLocalServiceWorkers() {
  if (!("serviceWorker" in navigator)) {
    return;
  }
  navigator.serviceWorker
    .getRegistrations()
    .then(registrations => {
      registrations.forEach(registration => {
        registration.unregister();
      });
    })
    .catch(() => {});
}

function activateWorker(registration) {
  const worker =
    registration && (registration.waiting || registration.installing);
  if (worker) {
    worker.postMessage({ type: "SKIP_WAITING" });
  }
}

function clearHomeCache() {
  if ("serviceWorker" in navigator && navigator.serviceWorker.controller) {
    navigator.serviceWorker.controller.postMessage({
      type: "CLEAR_HOME_CACHE"
    });
  }
}

export function registerServiceWorker() {
  try {
    if (window.getQueryString("nopwa") || isLocalServeHost()) {
      unregisterLocalServiceWorkers();
      return;
    }
    if (
      process.env.NODE_ENV === "production" &&
      !window.getQueryString("nopwa")
    ) {
      if ("serviceWorker" in navigator) {
        let refreshing = false;
        navigator.serviceWorker.addEventListener("controllerchange", () => {
          if (refreshing) {
            return;
          }
          refreshing = true;
          window.location.reload(true);
        });
      }
      register(`${process.env.BASE_URL}service-worker.js`, {
        ready() {
          // console.log(
          //   "App is being served from cache by a service worker.\n" +
          //     "For more details, visit https://goo.gl/AFskqB"
          // );
          window.serviceWorkerReady = true;
        },
        registered(registration) {
          // console.log("Service worker has been registered.");
          registration.update();
          registration.addEventListener("updatefound", () => {
            const newWorker = registration.installing;
            if (!newWorker) {
              return;
            }
            newWorker.addEventListener("statechange", () => {
              if (newWorker.state === "installed") {
                clearHomeCache();
                activateWorker(registration);
              }
            });
          });
          if (window.localStorage) {
            const currentVersion = window.localStorage.getItem(
              "READER_APP_BUILD_VERSION"
            );
            const newVersion = process.env.VUE_APP_BUILD_VERSION;
            if (currentVersion !== newVersion) {
              clearHomeCache();
              activateWorker(registration);
              window.localStorage.setItem(
                "READER_APP_BUILD_VERSION",
                newVersion
              );
            }
          }
        },
        updated(registration) {
          clearHomeCache();
          activateWorker(registration);
        }
        // cached() {
        //   console.log("Content has been cached for offline use.");
        // },
        // updatefound() {
        //   console.log("New content is downloading.");
        // },
        // updated() {
        //   console.log("New content is available; please refresh.");
        // },
        // offline() {
        //   console.log(
        //     "No internet connection found. App is running in offline mode."
        //   );
        // },
        // error(error) {
        //   console.error("Error during service worker registration:", error);
        // }
      });
    }
  } catch (error) {
    //
  }
}
