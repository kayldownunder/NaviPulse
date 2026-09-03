# NaviPulse Privacy Policy

_Last updated: 3 September 2026_

NaviPulse is a boat trip-tracking app. This policy explains what data it
accesses and, just as importantly, what it does not do with it.

## Data NaviPulse collects

- **Location.** While you have a trip running, NaviPulse uses your device's
  GPS to record your route, speed, and distance travelled. This continues in
  the background (via a foreground location service, shown as an ongoing
  notification) only while a trip is active.
- **Bluetooth connection state.** NaviPulse can detect when your phone
  connects to a car/boat head unit's Bluetooth audio profile, so it can
  auto-start or auto-stop trip tracking. It does not read any other
  Bluetooth device data.
- **Fuel and trip logs you enter.** Fuel-up amounts, prices, and any other
  details you type in are stored so the app can show your history and fuel
  economy.
- **App preferences.** Things like your chosen units, fonts, text sizes, and
  a custom dashboard background image you pick.

## Where this data lives

All of the above is stored **locally on your device only** (in the app's
private database and settings storage). NaviPulse has no backend server and
no user accounts - nothing is uploaded automatically, and nothing is shared
with the developer or any third party.

The only times data leaves the app are actions you explicitly take:

- **Backup / Restore** - exporting your trips and fuel logs to a JSON file
  you choose the location for (and restoring from one), using the standard
  Android file picker.
- **Export** - generating a CSV or PDF of your trip history, or opening a
  trip's route in Google Maps, both of which use Android's normal share/open
  mechanisms and only send the data to the app you pick.
- **Address lookups** - trip start/end coordinates are converted to
  human-readable addresses using Android's on-device Geocoder API, which may
  query network geocoding services depending on your device.

## What NaviPulse does not do

- No advertising, analytics, or tracking SDKs.
- No account creation, sign-in, or user profiles.
- No selling or sharing of your data with third parties.

## Deleting your data

You can delete individual trips or fuel logs from within the app at any
time. Uninstalling NaviPulse removes all of its locally stored data from
your device.

## Changes to this policy

If this policy changes, the update will be committed to this file in the
NaviPulse GitHub repository, so the history of changes is always visible.

## Contact

Questions about this policy can be raised via an issue on the
[NaviPulse GitHub repository](https://github.com/kayldownunder/NaviPulse).
